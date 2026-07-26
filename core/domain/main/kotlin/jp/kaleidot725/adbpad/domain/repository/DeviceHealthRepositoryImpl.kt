package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class DeviceHealthRepositoryImpl(
    private val settingRepository: SettingRepository,
) : DeviceHealthRepository {
    // ponytail: real adb binary, not the adam library - a hung device needs a real process
    // we can kill on timeout, and `adb reboot` needs to reach the daemon below whatever
    // is frozen at the UI layer.
    override suspend fun checkLiveness(device: Device): DeviceLiveness =
        withContext(Dispatchers.IO) {
            val adbPath = settingRepository.getSdkPath().adbDirectory.ifBlank { "adb" }
            var process: Process? = null
            try {
                withTimeout(LIVENESS_TIMEOUT) {
                    process = ProcessBuilder(adbPath, "-s", device.serial, "shell", "echo", "ok").start()
                    val exitCode = process!!.waitFor()
                    if (exitCode == 0) DeviceLiveness.RESPONSIVE else DeviceLiveness.UNRESPONSIVE
                }
            } catch (e: TimeoutCancellationException) {
                process?.destroyForcibly()
                DeviceLiveness.UNRESPONSIVE
            } catch (e: Exception) {
                DeviceLiveness.UNRESPONSIVE
            }
        }

    override suspend fun restart(device: Device) {
        withContext(Dispatchers.IO) {
            val adbPath = settingRepository.getSdkPath().adbDirectory.ifBlank { "adb" }
            try {
                ProcessBuilder(adbPath, "-s", device.serial, "reboot").start()
            } catch (e: Exception) {
                // best-effort - nothing more we can do if the process can't even start
            }
        }
    }

    companion object {
        private const val LIVENESS_TIMEOUT = 5_000L
    }
}
