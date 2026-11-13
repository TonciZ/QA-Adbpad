package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface LogCaptureRepository {
    val isCapturing: StateFlow<Boolean>
    val logLines: Flow<String>

    /** Lines written since the last [startCapture]; the on-screen list is capped, this is not. */
    val capturedLineCount: Long

    fun startCapture(
        device: Device,
        filter: String = "",
    )

    /** Stops logcat. The capture is kept until the next [startCapture] or [discardCapture]. */
    suspend fun stopCapture()

    /** Writes [lines] to [target], or the whole capture when [lines] is null. False when it could not be written. */
    suspend fun saveCapture(
        target: File,
        lines: List<String>?,
    ): Boolean

    fun discardCapture()
}
