package jp.kaleidot725.adbpad.domain.usecase.screenshot

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.screenshot.Screenshot
import jp.kaleidot725.adbpad.domain.repository.ScreenshotCommandRepository

class TakeScreenshotUseCase(
    private val screenshotCommandRepository: ScreenshotCommandRepository,
) {
    suspend operator fun invoke(
        device: Device,
        onStart: suspend () -> Unit,
        onFailed: suspend () -> Unit,
        onComplete: suspend (Screenshot) -> Unit,
    ) {
        screenshotCommandRepository.captureScreenshot(
            device = device,
            onStart = { onStart() },
            onFailed = { onFailed() },
            onComplete = { onComplete(it) },
        )
    }
}
