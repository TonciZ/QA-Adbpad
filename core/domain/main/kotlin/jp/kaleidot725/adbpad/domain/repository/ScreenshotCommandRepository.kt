package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.screenshot.Screenshot
import jp.kaleidot725.adbpad.domain.model.sort.SortType

interface ScreenshotCommandRepository {
    suspend fun captureScreenshot(
        device: Device,
        onStart: suspend () -> Unit,
        onComplete: suspend (Screenshot) -> Unit,
        onFailed: suspend () -> Unit,
    )

    suspend fun getScreenshots(
        searchText: String,
        sortType: SortType,
    ): List<Screenshot>

    suspend fun rename(
        screenshot: Screenshot,
        name: String,
    ): Boolean

    suspend fun delete(screenshot: Screenshot)

    suspend fun deleteAll(screenshots: List<Screenshot>)
}
