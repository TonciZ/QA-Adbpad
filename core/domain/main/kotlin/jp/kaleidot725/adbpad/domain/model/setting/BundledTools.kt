package jp.kaleidot725.adbpad.domain.model.setting

import java.io.File

/**
 * adb and scrcpy shipped inside the installed app (see `prepareBundledTools` in the root
 * build.gradle.kts), so the app works without the user installing or locating either one.
 * Both are null when running a build that carries no bundle.
 */
object BundledTools {
    // Set by the Compose Desktop launcher, for packaged builds and `./gradlew run` alike.
    private const val RESOURCES_DIR_PROPERTY = "compose.application.resources.dir"

    private val toolsDir: File? by lazy {
        System.getProperty(RESOURCES_DIR_PROPERTY)?.let { File(it, "tools") }?.takeIf { it.isDirectory }
    }

    val adb: File? by lazy { find("adb") }
    val scrcpy: File? by lazy { find("scrcpy") }

    private fun find(name: String): File? {
        val dir = toolsDir ?: return null
        return listOf("$name.exe", name).map { File(dir, it) }.firstOrNull { it.isFile }
    }
}
