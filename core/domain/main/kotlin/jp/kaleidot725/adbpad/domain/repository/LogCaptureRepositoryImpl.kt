package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.os.OSContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogCaptureRepositoryImpl(
    private val settingRepository: SettingRepository,
) : LogCaptureRepository {
    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing

    private val _logLines = MutableSharedFlow<String>(extraBufferCapacity = 1000)
    override val logLines: Flow<String> = _logLines

    @Volatile private var process: Process? = null
    private var readerJob: Job? = null

    // Lines stream straight to this temp file; an in-memory list would grow unbounded.
    private var captureFile: File? = null
    private var hasCapturedLines = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startCapture(
        device: Device,
        filter: String,
    ) {
        if (_isCapturing.value) return

        hasCapturedLines = false
        _isCapturing.value = true

        readerJob =
            scope.launch {
                var writer: BufferedWriter? = null
                try {
                    val extraArgs = mutableListOf("-s", device.serial, "logcat", "-v", "time")
                    if (filter.isNotBlank()) {
                        extraArgs.addAll(filter.split(" "))
                    }

                    val pb = AdbBinary.processBuilder(settingRepository, *extraArgs.toTypedArray())
                    pb.redirectErrorStream(true)
                    val started = pb.start()
                    process = started
                    if (!currentCoroutineContext().isActive) {
                        started.destroyForcibly()
                        return@launch
                    }

                    val file = File.createTempFile("adbpad_logcat_", ".txt")
                    file.deleteOnExit()
                    captureFile = file
                    writer = file.bufferedWriter()

                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        writer.write(l)
                        writer.newLine()
                        hasCapturedLines = true
                        _logLines.emit(l)
                    }
                } catch (_: Exception) {
                } finally {
                    writer?.close()
                    _isCapturing.value = false
                }
            }
    }

    override suspend fun stopCapture(): File? {
        process?.destroyForcibly()
        process = null
        // Join so the temp file is flushed and closed before it's read back below.
        readerJob?.cancelAndJoin()
        readerJob = null
        _isCapturing.value = false

        val tempFile = captureFile
        captureFile = null
        if (!hasCapturedLines || tempFile == null || !tempFile.exists()) return null
        hasCapturedLines = false

        // ponytail: was a fixed OSContext path before - now asks where to save every time,
        // like any normal "Save As" dialog, defaulting to the same folder as a starting point.
        val osContext = OSContext.resolveOSContext()
        val logDir = File(osContext.directory + "logs/")
        logDir.mkdirs()

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val dialog = FileDialog(null as java.awt.Frame?, "Save Log As", FileDialog.SAVE)
        dialog.directory = logDir.absolutePath
        dialog.file = "logcat_$timestamp.txt"
        dialog.isVisible = true

        val chosenDir = dialog.directory ?: return null
        val chosenFile = dialog.file ?: return null
        val file = File(chosenDir, chosenFile)
        tempFile.copyTo(file, overwrite = true)
        tempFile.delete()
        return file
    }
}
