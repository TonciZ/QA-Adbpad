package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.device.FetchDeviceFeaturesRequest
import com.malinskiy.adam.request.framebuffer.RawImageScreenCaptureAdapter
import com.malinskiy.adam.request.framebuffer.ScreenCaptureRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.PullRequest
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.os.OSContext
import jp.kaleidot725.adbpad.domain.model.screenshot.Screenshot
import jp.kaleidot725.adbpad.domain.model.sort.SortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Date
import javax.imageio.ImageIO

class ScreenshotCommandRepositoryImpl : ScreenshotCommandRepository {
    private val adb = AndroidDebugBridgeClientFactory().build()

    init {
        createDirectory()
    }

    override suspend fun captureScreenshot(
        device: Device,
        onStart: suspend () -> Unit,
        onComplete: suspend (Screenshot) -> Unit,
        onFailed: suspend () -> Unit,
    ) {
        val date = Date()
        withContext(Dispatchers.IO) {
            onStart()
            val result = capture(device, getFileResult(date.time))
            if (result) onComplete(Screenshot(getFileResult(date.time))) else onFailed()
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
                    .map { file -> Screenshot(file) }
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

    private suspend fun capture(
        device: Device,
        file: File,
    ): Boolean {
        return try {
            withTimeout(CAPTURE_TIMEOUT) {
                val adapter = RawImageScreenCaptureAdapter()
                val image = adb.execute(request = ScreenCaptureRequest(adapter), serial = device.serial).toBufferedImage()
                ImageIO.write(image, EXTENSION_NAME, file)
            }
        } catch (e: TimeoutCancellationException) {
            captureFallback(device, file)
        }
    }

    private suspend fun captureFallback(
        device: Device,
        file: File,
    ): Boolean {
        return try {
            withTimeout(CAPTURE_TIMEOUT) {
                val remotePath = "/data/local/tmp/adbpad_screenshot.png"
                val result = adb.execute(ShellCommandRequest("screencap -p $remotePath"), device.serial)
                if (result.exitCode != 0) return@withTimeout false
                val supportedFeatures = adb.execute(FetchDeviceFeaturesRequest(device.serial), device.serial)
                val pulled = adb.execute(PullRequest(remotePath, file, supportedFeatures), device.serial)
                adb.execute(ShellCommandRequest("rm -f $remotePath"), device.serial)
                pulled && file.exists() && file.length() > 0
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val FILE_NAME_RESULT = "Screenshot"
        private const val EXTENSION_NAME = "png"
        private const val CAPTURE_TIMEOUT = 10_000L

        private fun createDirectory() {
            getDirectory().mkdir()
        }

        private fun getDirectory(): File {
            val osContext = OSContext.resolveOSContext()
            return File(osContext.screenshotDirectory)
        }

        private fun getFileResult(time: Long): File {
            val osContext = OSContext.resolveOSContext()
            val fileName = "${FILE_NAME_RESULT}_$time.png"
            return File(osContext.screenshotDirectory + fileName)
        }
    }
}
