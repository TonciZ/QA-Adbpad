package jp.kaleidot725.adbpad.domain.usecase.scrcpy

import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets
import jp.kaleidot725.adbpad.domain.repository.SettingRepository

class GetScrcpyTierPresetsUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(): ScrcpyTierPresets = settingRepository.getScrcpyTierPresets()
}
