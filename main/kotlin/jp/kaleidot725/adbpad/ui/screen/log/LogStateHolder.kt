package jp.kaleidot725.adbpad.ui.screen.log

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.log.LogEntry
import jp.kaleidot725.adbpad.domain.repository.LogCaptureRepository
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.ui.container.AppBroadCast
import jp.kaleidot725.adbpad.ui.container.AppUnicast
import jp.kaleidot725.adbpad.ui.screen.log.state.LogAction
import jp.kaleidot725.adbpad.ui.screen.log.state.LogState
import jp.kaleidot725.pulse.mvi.PulseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LogStateHolder(
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val logCaptureRepository: LogCaptureRepository,
) : PulseStore<LogState, LogAction, Nothing, AppBroadCast, AppUnicast>(initialUiState = LogState()) {
    private var selectedDevice: Device? = null
    private var logCollectorJob: Job? = null
    private val pending = mutableListOf<LogEntry>()
    private var nextId = 0L

    override fun onSetup() {
        coroutineScope.launch {
            getSelectedDeviceFlowUseCase().collectLatest { device ->
                if (selectedDevice != null && device?.serial != selectedDevice?.serial) {
                    stopCapture()
                }
                selectedDevice = device
            }
        }

        coroutineScope.launch {
            logCaptureRepository.isCapturing.collectLatest { capturing ->
                update { copy(isCapturing = capturing) }
            }
        }
    }

    override fun onAction(uiAction: LogAction) {
        coroutineScope.launch {
            when (uiAction) {
                LogAction.StartCapture -> startCapture()
                LogAction.StopCapture -> stopCapture()
                LogAction.ClearLog -> update { copy(entries = emptyList(), savedFile = "") }
                is LogAction.UpdateFilter -> update { copy(filter = uiAction.filter) }
                is LogAction.UpdateSearch -> update { copy(search = uiAction.search) }
                is LogAction.UpdateMinLevel -> update { copy(minLevel = uiAction.level) }
                is LogAction.SetAutoScroll -> update { copy(autoScroll = uiAction.enabled) }
                is LogAction.SetWrapLines -> update { copy(wrapLines = uiAction.enabled) }
            }
        }
    }

    override fun onReceive(broadcast: AppBroadCast) {}

    private fun startCapture() {
        val device = selectedDevice ?: return
        update { copy(entries = emptyList(), savedFile = "") }
        synchronized(pending) { pending.clear() }

        logCaptureRepository.startCapture(device, currentState.filter)

        logCollectorJob?.cancel()
        logCollectorJob =
            coroutineScope.launch {
                launch {
                    logCaptureRepository.logLines.collect { line ->
                        synchronized(pending) { pending.add(LogEntry.parse(nextId++, line)) }
                    }
                }
                // ponytail: logcat can emit thousands of lines per second - copying the whole list
                // for every line froze the UI, so lines are buffered and published a few times a second.
                launch {
                    while (isActive) {
                        delay(FLUSH_INTERVAL)
                        flushPending()
                    }
                }
            }
    }

    private fun flushPending() {
        val batch = synchronized(pending) { pending.toList().also { pending.clear() } }
        if (batch.isEmpty()) return
        update {
            val newEntries = entries + batch
            val trimmed = if (newEntries.size > MAX_LINES) newEntries.drop(newEntries.size - MAX_LINES) else newEntries
            copy(entries = trimmed)
        }
    }

    private fun stopCapture() {
        logCollectorJob?.cancel()
        logCollectorJob = null
        flushPending()
        val file = logCaptureRepository.stopCapture()
        update { copy(isCapturing = false, savedFile = file?.absolutePath ?: "") }
    }

    companion object {
        private const val FLUSH_INTERVAL = 200L
        private const val MAX_LINES = 10_000
    }
}
