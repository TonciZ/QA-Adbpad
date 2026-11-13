package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
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
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader

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

    @Volatile
    private var writtenLines: Long = 0

    // Lines at the start of the capture file that the user cleared away mid-capture.
    @Volatile
    private var clearedLines: Long = 0

    override val capturedLineCount: Long
        get() = writtenLines - clearedLines
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startCapture(
        device: Device,
        filter: String,
    ) {
        if (_isCapturing.value) return

        discardCapture()
        _isCapturing.value = true

        readerJob =
            scope.launch {
                var writer: BufferedWriter? = null
                var started: Process? = null
                try {
                    val extraArgs = mutableListOf("-s", device.serial, "logcat", "-v", "time")
                    if (filter.isNotBlank()) {
                        extraArgs.addAll(filter.split(" "))
                    }

                    val pb = AdbBinary.processBuilder(settingRepository, *extraArgs.toTypedArray())
                    pb.redirectErrorStream(true)
                    val logcat = pb.start()
                    started = logcat
                    process = logcat
                    if (!currentCoroutineContext().isActive) return@launch

                    val file = File.createTempFile("adbpad_logcat_", ".txt")
                    file.deleteOnExit()
                    captureFile = file
                    writer = file.bufferedWriter()

                    val reader = BufferedReader(InputStreamReader(logcat.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        writer.write(l)
                        writer.newLine()
                        writtenLines++
                        _logLines.emit(l)
                    }
                } catch (e: Exception) {
                    println("Log capture stopped: ${e.message}")
                } finally {
                    runCatching { writer?.close() }
                    // However this ended (device gone, disk full, cancelled), logcat must not be
                    // left running with nobody reading it.
                    started?.destroyForcibly()
                    if (process === started) process = null
                    _isCapturing.value = false
                }
            }
    }

    override suspend fun stopCapture() {
        process?.destroyForcibly()
        process = null
        // Join so the temp file is flushed and closed before anything reads it back.
        readerJob?.cancelAndJoin()
        readerJob = null
        _isCapturing.value = false
    }

    override suspend fun saveCapture(
        target: File,
        lines: List<String>?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                target.parentFile?.mkdirs()
                val source = captureFile
                when {
                    lines != null -> target.writeText(lines.joinToString(System.lineSeparator()))
                    source != null && source.exists() && clearedLines > 0 ->
                        source.bufferedReader().use { reader ->
                            target.bufferedWriter().use { out ->
                                reader.lineSequence().drop(clearedLines.toInt()).forEach {
                                    out.write(it)
                                    out.newLine()
                                }
                            }
                        }

                    source != null && source.exists() -> source.copyTo(target, overwrite = true)
                    else -> return@withContext false
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    override fun markCleared() {
        clearedLines = writtenLines
    }

    override fun discardCapture() {
        captureFile?.delete()
        captureFile = null
        writtenLines = 0
        clearedLines = 0
    }
}
