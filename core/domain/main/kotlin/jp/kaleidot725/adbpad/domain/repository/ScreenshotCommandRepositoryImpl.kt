package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.os.OSContext
import jp.kaleidot725.adbpad.domain.model.screenshot.Screenshot
import jp.kaleidot725.adbpad.domain.model.sort.SortType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class ScreenshotCommandRepositoryImpl(
    private val settingRepository: SettingRepository,
) : ScreenshotCommandRepository {
    override suspend fun captureScreenshot(
        device: Device,
        onStart: suspend () -> Unit,
        onComplete: suspend (Screenshot) -> Unit,
        onFailed: suspend () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            onStart()
            val deviceLabel = sanitizeDeviceLabel(device.displayName)
            val file = getFileResult(Date().time, deviceLabel)
            val result = capture(device, file)
            if (result) onComplete(Screenshot(file, deviceLabel)) else onFailed()
        }
    }

    override suspend fun getScreenshots(
        searchText: String,
        sortType: SortType,
    ): List<Screenshot> =
        withContext(Dispatchers.IO) {
            val files = getDirectory().listFiles() ?: emptyArray()
            val filteredFiles =
                files
                    .filter { file -> file.isFile }
                    .map { file -> Screenshot(file, parseDeviceLabel(file.name)) }
                    .filter { (it.file?.name ?: "").startsWith(searchText) }
            when (sortType) {
                SortType.SORT_BY_NAME_ASC -> {
                    filteredFiles.sortedBy { screenshot -> screenshot.file?.name ?: "" }
                }

                SortType.SORT_BY_NAME_DESC -> {
                    filteredFiles.sortedByDescending { screenshot -> screenshot.file?.name ?: "" }
                }
            }
        }

    override suspend fun rename(
        screenshot: Screenshot,
        name: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val file = screenshot.file ?: return@withContext false
            if (!file.exists()) return@withContext false

            val extension = file.extension
            val newName = if (name.endsWith(".$extension")) name else "$name.$extension"
            val newFile = File(file.parent, newName)

            file.renameTo(newFile)
        }
    }

    override suspend fun delete(screenshot: Screenshot) {
        withContext(Dispatchers.IO) {
            screenshot.file?.delete()
        }
    }

    override suspend fun deleteAll(screenshots: List<Screenshot>) {
        withContext(Dispatchers.IO) {
            screenshots.forEach { it.file?.delete() }
        }
    }

    // ponytail: shell out to the real adb binary instead of the adam library's reimplementation
    // of the sync/framebuffer protocol - adam silently no-ops on some OEM (Android TV) daemons
    // that misreport protocol feature support. `adb exec-out` is what works everywhere.
    private suspend fun capture(
        device: Device,
        file: File,
    ): Boolean {
        var process: Process? = null
        return try {
            // ponytail: create the folder here, not once at startup. On a fresh install the
            // app's own data folder does not exist yet when this class is built, the old
            // single-level mkdir() failed silently, and every capture then failed too.
            // Only when it is missing: first capture after install, or the user deleted it.
            file.parentFile?.let { folder -> if (!folder.isDirectory) folder.mkdirs() }
            coroutineScope {
                val adb =
                    AdbBinary
                        .processBuilder(settingRepository, "-s", device.serial, "exec-out", "screencap", "-p")
                        .start()
                process = adb
                // Reading the pipe blocks and ignores coroutine timeouts; ending the process is
                // what actually unblocks it when a device stops answering.
                val watchdog =
                    launch {
                        delay(CAPTURE_TIMEOUT)
                        adb.destroyForcibly()
                    }
                try {
                    file.outputStream().use { out -> adb.inputStream.copyTo(out) }
                    adb.waitFor() == 0 && file.length() > 0
                } finally {
                    watchdog.cancel()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Screenshot failed: ${e.message}")
            false
        } finally {
            // Never leave `adb screencap` running after a failed or abandoned capture.
            process?.destroyForcibly()
        }.also { captured -> if (!captured) file.delete() }
    }

    companion object {
        private const val FILE_NAME_RESULT = "Screenshot"
        private const val CAPTURE_TIMEOUT = 10_000L

        private fun getDirectory(): File {
            val osContext = OSContext.resolveOSContext()
            return File(osContext.screenshotDirectory)
        }

        private fun getFileResult(
            time: Long,
            deviceLabel: String,
        ): File {
            val osContext = OSContext.resolveOSContext()
            val fileName = "${FILE_NAME_RESULT}_${time}_$deviceLabel.png"
            return File(osContext.screenshotDirectory + fileName)
        }

        // ponytail: device label is round-tripped through the filename, so a device name with
        // symbols renders slightly mangled on re-list - acceptable, avoids a sidecar metadata file.
        private fun sanitizeDeviceLabel(label: String): String = label.replace(Regex("[^A-Za-z0-9.-]"), "-")

        private fun parseDeviceLabel(fileName: String): String? {
            val parts = fileName.substringBeforeLast('.').split('_')
            return if (parts.size >= 3) parts.drop(2).joinToString("_") else null
        }
    }
}
