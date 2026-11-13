package jp.kaleidot725.adbpad.domain.usecase.adb

import jp.kaleidot725.adbpad.domain.repository.AdbBinary
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * The adb server outlives the app. When it was started from the adb inside the install folder
 * it keeps those files locked, which blocks the installer on update or uninstall - so stop it.
 * A server started from the user's own adb (custom path in Settings) is left alone.
 */
class StopBundledAdbServerUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke() {
        withContext(Dispatchers.IO) {
            try {
                if (!settingRepository.getSdkPath().usesBundledAdb) return@withContext
                val process = AdbBinary.processBuilder(settingRepository, "kill-server").start()
                if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) process.destroyForcibly()
            } catch (_: Exception) {
                // best-effort: exiting must never be blocked by adb
            }
        }
    }

    companion object {
        private const val TIMEOUT_SECONDS = 3L
    }
}
