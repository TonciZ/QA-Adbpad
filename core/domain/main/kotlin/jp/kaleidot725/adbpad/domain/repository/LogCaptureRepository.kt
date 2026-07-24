package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface LogCaptureRepository {
    val isCapturing: StateFlow<Boolean>
    val logLines: Flow<String>
    fun startCapture(device: Device, filter: String = "")
    fun stopCapture(): File?
}
