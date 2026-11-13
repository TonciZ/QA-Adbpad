package jp.kaleidot725.adbpad.ui.screen.log

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.log.LogEntry
import jp.kaleidot725.adbpad.domain.model.os.OSContext
import jp.kaleidot725.adbpad.domain.repository.LogCaptureRepository
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.ui.container.AppBroadCast
import jp.kaleidot725.adbpad.ui.container.AppUnicast
import jp.kaleidot725.adbpad.ui.screen.log.state.LogAction
import jp.kaleidot725.adbpad.ui.screen.log.state.LogSaveDialogState
import jp.kaleidot725.adbpad.ui.screen.log.state.LogState
import jp.kaleidot725.pulse.mvi.PulseStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.UIManager

class LogStateHolder(
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val logCaptureRepository: LogCaptureRepository,
    private val settingRepository: SettingRepository,
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
                update { copy(hasDevice = device != null) }
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
                LogAction.ClearLog -> clearLog()
                LogAction.OpenSaveDialog -> openSaveDialog()
                LogAction.CloseSaveDialog -> update { copy(saveDialog = null) }
                is LogAction.UpdateSaveFileName -> update { copy(saveDialog = saveDialog?.copy(fileName = uiAction.fileName)) }
                is LogAction.SetSaveOnlyShown -> update { copy(saveDialog = saveDialog?.copy(onlyShown = uiAction.onlyShown)) }
                LogAction.ChooseSaveDirectory -> chooseSaveDirectory()
                LogAction.ConfirmSave -> confirmSave()
                LogAction.OpenSavedFolder -> openSavedFolder()
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

    private suspend fun stopCapture() {
        logCollectorJob?.cancel()
        logCollectorJob = null
        flushPending()
        logCaptureRepository.stopCapture()
        update { copy(isCapturing = false) }
    }

    private fun clearLog() {
        if (!currentState.isCapturing) logCaptureRepository.discardCapture()
        update { copy(entries = emptyList(), savedFile = "") }
    }

    private suspend fun openSaveDialog() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        // The device in the name saves a tester from renaming files before sending them on.
        val device = selectedDevice?.displayName?.replace(INVALID_FILE_CHARS, "-")?.takeIf { it.isNotBlank() }
        val name = listOfNotNull("logcat", device, timestamp).joinToString("_") + ".txt"
        val directory = settingRepository.getLogSaveDirectory().takeIf { it.isNotBlank() && File(it).isDirectory } ?: defaultDirectory()
        val shown = currentState.visibleEntries.size
        update {
            copy(
                saveDialog =
                    LogSaveDialogState(
                        fileName = name,
                        directory = directory,
                        totalLines = maxOf(logCaptureRepository.capturedLineCount, entries.size.toLong()),
                        shownLines = shown,
                        canSaveShownOnly = isFiltered,
                    ),
            )
        }
    }

    // ponytail: the old default was the hidden app-data folder, which testers could not find
    // again. Documents is where people look; fall back to app data when there is none.
    private fun defaultDirectory(): String {
        val documents = File(System.getProperty("user.home"), "Documents")
        return if (documents.isDirectory) {
            File(documents, "QA-Adbpad Logs").absolutePath
        } else {
            File(OSContext.resolveOSContext().directory, "logs").absolutePath
        }
    }

    private suspend fun chooseSaveDirectory() {
        val dialog = currentState.saveDialog ?: return
        val chosen =
            withContext(Dispatchers.Main) {
                runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
                val chooser = JFileChooser(File(dialog.directory).takeIf { it.isDirectory } ?: File(System.getProperty("user.home")))
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
            } ?: return
        update { copy(saveDialog = saveDialog?.copy(directory = chosen, failed = false)) }
    }

    private suspend fun confirmSave() {
        val dialog = currentState.saveDialog ?: return
        val cleaned =
            dialog.fileName
                .trim()
                .replace(INVALID_FILE_CHARS, "-")
                .ifBlank { "logcat" }
        val target = File(dialog.directory, if (cleaned.contains('.')) cleaned else "$cleaned.txt")
        val lines = if (dialog.onlyShown) currentState.visibleEntries.map { it.raw } else null
        // Without a capture file (e.g. after a restart of the capture) fall back to what is on screen.
        val saved =
            logCaptureRepository.saveCapture(target, lines) ||
                (lines == null && logCaptureRepository.saveCapture(target, currentState.entries.map { it.raw }))
        if (saved) {
            settingRepository.updateLogSaveDirectory(dialog.directory)
            update { copy(saveDialog = null, savedFile = target.absolutePath) }
        } else {
            update { copy(saveDialog = saveDialog?.copy(failed = true)) }
        }
    }

    private suspend fun openSavedFolder() {
        val folder = File(currentState.savedFile).parentFile ?: return
        withContext(Dispatchers.IO) {
            runCatching { Desktop.getDesktop().open(folder) }
        }
    }

    companion object {
        private const val FLUSH_INTERVAL = 200L
        private const val MAX_LINES = 10_000
        private val INVALID_FILE_CHARS = Regex("""[\\/:*?"<>|\s]+""")
    }
}
