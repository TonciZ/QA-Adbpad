package jp.kaleidot725.adbpad.domain.model.os

import java.io.File

sealed class OSContext(
    val directory: String = "",
) {
    val screenshotDirectory get() = "${directory}screenshot/"

    object Mac : OSContext(System.getProperty("user.home") + "/Library/Application Support/adbpad/")

    object Windows : OSContext(System.getProperty("user.home") + "/adbpad/")

    object Linux : OSContext(linuxDirectory())

    companion object {
        fun resolveOSContext(): OSContext {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.startsWith("mac") -> Mac
                os.startsWith("windows") -> Windows
                else -> Linux
            }
        }

        // ponytail: Linux used to share the Windows branch (~/adbpad). Keep using that folder
        // when it already holds data so nobody loses settings; new installs follow XDG.
        private fun linuxDirectory(): String {
            val home = System.getProperty("user.home")
            val legacy = "$home/adbpad/"
            if (File(legacy, "config.json").isFile) return legacy
            val configHome = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.config"
            return "$configHome/adbpad/"
        }
    }
}
