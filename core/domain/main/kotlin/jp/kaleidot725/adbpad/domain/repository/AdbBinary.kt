package jp.kaleidot725.adbpad.domain.repository

// ponytail: every repo that shells out to the real adb binary must agree on the same
// server port the app itself configured, or commands silently hit the wrong (or no)
// server. One shared builder instead of four places to forget `-P`.
object AdbBinary {
    suspend fun processBuilder(
        settingRepository: SettingRepository,
        vararg args: String,
    ): ProcessBuilder {
        val sdkPath = settingRepository.getSdkPath()
        val adbPath = sdkPath.adbDirectory.ifBlank { "adb" }
        return ProcessBuilder(listOf(adbPath, "-P", sdkPath.adbServerPort.toString()) + args)
    }
}
