package jp.kaleidot725.adbpad.domain.model.setting

import kotlinx.serialization.Serializable

@Serializable
data class ScrcpySettings(
    val binaryPath: String = "",
) {
    /** The scrcpy to run: the path from Settings if one is set, otherwise the bundled scrcpy. */
    val executable: String
        get() =
            binaryPath.takeIf { it.isNotBlank() && java.io.File(it).isFile }
                ?: BundledTools.scrcpy?.path
                ?: binaryPath
}
