package jp.kaleidot725.adbpad.data.repository

import jp.kaleidot725.adbpad.data.local.SettingFileCreator
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.AccentColor
import jp.kaleidot725.adbpad.domain.model.setting.Appearance
import jp.kaleidot725.adbpad.domain.model.setting.ScrcpySettings
import jp.kaleidot725.adbpad.domain.model.setting.SdkPath
import jp.kaleidot725.adbpad.domain.model.setting.WindowSize
import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingRepositoryImpl : SettingRepository {
    override suspend fun updateAppearance(appearance: Appearance): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(appearance = appearance) }
        }
    }

    override suspend fun getAppearance(): Appearance {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.appearance
        }
    }

    override suspend fun updateSdkPath(sdkPath: SdkPath): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(sdkPath = sdkPath) }
        }
    }

    override suspend fun getSdkPath(): SdkPath {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.sdkPath
        }
    }

    override suspend fun updateWindowSize(size: WindowSize): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(windowSize = size) }
        }
    }

    override suspend fun getWindowSize(): WindowSize {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.windowSize
        }
    }

    override suspend fun updateLanguage(language: Language.Type): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(language = language) }
        }
    }

    override suspend fun getLanguage(): Language.Type {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.language
        }
    }

    override suspend fun updateScrcpySettings(scrcpySettings: ScrcpySettings): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(scrcpySettings = scrcpySettings) }
        }
    }

    override suspend fun getScrcpySettings(): ScrcpySettings {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.scrcpySettings
        }
    }

    override suspend fun updateAccentColor(accentColor: AccentColor): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(accentColor = accentColor) }
        }
    }

    override suspend fun getAccentColor(): AccentColor {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.accentColor
        }
    }

    override suspend fun updateScrcpyTierPresets(presets: ScrcpyTierPresets): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(scrcpyTierPresets = presets) }
        }
    }

    override suspend fun getScrcpyTierPresets(): ScrcpyTierPresets {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.scrcpyTierPresets
        }
    }

    override suspend fun updateLastWirelessAdbTarget(target: WirelessAdbTarget): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(lastWirelessAdbTarget = target) }
        }
    }

    override suspend fun getLastWirelessAdbTarget(): WirelessAdbTarget {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            return@withContext setting.lastWirelessAdbTarget
        }
    }

    override suspend fun updateSavedWirelessDevices(devices: List<WirelessAdbTarget>): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(savedWirelessDevices = devices) }
        }
    }

    override suspend fun getSavedWirelessDevices(): List<WirelessAdbTarget> {
        return withContext(Dispatchers.IO) {
            val setting = SettingFileCreator.load()
            // Before the list existed only the last target was remembered - carry it over.
            val last = setting.lastWirelessAdbTarget
            return@withContext setting.savedWirelessDevices.ifEmpty { if (last.host.isBlank()) emptyList() else listOf(last) }
        }
    }

    override suspend fun updateCustomCommandHistory(history: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(customCommandHistory = history) }
        }
    }

    override suspend fun getCustomCommandHistory(): List<String> {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.load().customCommandHistory
        }
    }

    override suspend fun updateLogSaveDirectory(directory: String): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.update { it.copy(logSaveDirectory = directory) }
        }
    }

    override suspend fun getLogSaveDirectory(): String {
        return withContext(Dispatchers.IO) {
            return@withContext SettingFileCreator.load().logSaveDirectory
        }
    }
}
