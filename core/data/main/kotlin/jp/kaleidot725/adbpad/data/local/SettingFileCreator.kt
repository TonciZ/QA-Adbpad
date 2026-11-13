package jp.kaleidot725.adbpad.data.local

import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.AccentColor
import jp.kaleidot725.adbpad.domain.model.setting.Appearance
import jp.kaleidot725.adbpad.domain.model.setting.ScrcpySettings
import jp.kaleidot725.adbpad.domain.model.setting.SdkPath
import jp.kaleidot725.adbpad.domain.model.setting.WindowSize
import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SettingFileCreator {
    private const val FILE_NAME = "config.json"

    // ponytail: without this, one unknown key (config saved by a newer version) makes load()
    // fall back to an all-default Setting, i.e. every saved path and preference is "forgotten".
    private val json = Json { ignoreUnknownKeys = true }

    private val lock = Any()

    /**
     * Changes one part of the settings. Every feature shares this one file, so the
     * read-change-write has to be a single step or two features saving at the same moment
     * would silently undo each other.
     */
    fun update(change: (Setting) -> Setting): Boolean = synchronized(lock) { save(change(load())) }

    fun save(setting: Setting): Boolean =
        synchronized(lock) {
            try {
                FilePathUtil.createDir()
                val target = FilePathUtil.getFilePath(FILE_NAME)
                // Write next to the file and swap it in, so a reader (or a crash) never sees a
                // half-written config - that would be read back as "all defaults".
                val temp = File(target.parentFile, "$FILE_NAME.tmp")
                temp.writeText(Json.encodeToString(setting))
                try {
                    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                true
            } catch (_: IOException) {
                false
            }
        }

    fun load(): Setting =
        synchronized(lock) {
            try {
                val content = FilePathUtil.getFilePath(FILE_NAME).readText()
                json.decodeFromString(string = content)
            } catch (_: Exception) {
                Setting()
            }
        }

    @Serializable
    data class Setting(
        val language: Language.Type = Language.Type.ENGLISH,
        val appearance: Appearance = Appearance.LIGHT,
        val sdkPath: SdkPath = SdkPath(),
        val windowSize: WindowSize = WindowSize.DEFAULT,
        val scrcpySettings: ScrcpySettings = ScrcpySettings(),
        val accentColor: AccentColor = AccentColor.BLUE,
        val scrcpyTierPresets: ScrcpyTierPresets = ScrcpyTierPresets(),
        val lastWirelessAdbTarget: WirelessAdbTarget = WirelessAdbTarget(),
        val savedWirelessDevices: List<WirelessAdbTarget> = emptyList(),
        val customCommandHistory: List<String> = emptyList(),
        val logSaveDirectory: String = "",
    )
}
