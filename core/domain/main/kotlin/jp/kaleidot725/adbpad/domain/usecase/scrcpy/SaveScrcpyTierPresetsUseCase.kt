package jp.kaleidot725.adbpad.domain.usecase.scrcpy

import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets
import jp.kaleidot725.adbpad.domain.repository.SettingRepository

class SaveScrcpyTierPresetsUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(presets: ScrcpyTierPresets): Boolean = settingRepository.updateScrcpyTierPresets(presets)
}
