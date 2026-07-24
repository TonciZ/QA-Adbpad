package jp.kaleidot725.adbpad.ui.screen.log

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.repository.LogCaptureRepository
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.ui.container.AppBroadCast
import jp.kaleidot725.adbpad.ui.container.AppUnicast
import jp.kaleidot725.adbpad.ui.screen.log.state.LogAction
import jp.kaleidot725.adbpad.ui.screen.log.state.LogState
import jp.kaleidot725.pulse.mvi.PulseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogStateHolder(
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val logCaptureRepository: LogCaptureRepository,
) : PulseStore<LogState, LogAction, Nothing, AppBroadCast, AppUnicast>(initialUiState = LogState()) {
    private var selectedDevice: Device? = null
    private var logCollectorJob: Job? = null

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
                LogAction.ClearLog -> update { copy(lines = emptyList(), savedFile = "") }
                is LogAction.UpdateFilter -> update { copy(filter = uiAction.filter) }
            }
        }
    }

    override fun onReceive(broadcast: AppBroadCast) {}

    private fun startCapture() {
        val device = selectedDevice ?: return
        update { copy(lines = emptyList(), savedFile = "") }

        logCaptureRepository.startCapture(device, currentState.filter)

        logCollectorJob?.cancel()
        logCollectorJob = coroutineScope.launch {
            logCaptureRepository.logLines.collect { line ->
                update {
                    val newLines = lines + line
                    // ponytail: cap at 10k lines, drop oldest if over
                    val trimmed = if (newLines.size > 10_000) newLines.drop(newLines.size - 10_000) else newLines
                    copy(lines = trimmed)
                }
            }
        }
    }

    private fun stopCapture() {
        logCollectorJob?.cancel()
        logCollectorJob = null
        val file = logCaptureRepository.stopCapture()
        update { copy(isCapturing = false, savedFile = file?.absolutePath ?: "") }
    }
}
