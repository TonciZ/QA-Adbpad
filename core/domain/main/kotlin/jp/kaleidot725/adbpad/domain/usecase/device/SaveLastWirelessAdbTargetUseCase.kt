package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.domain.repository.SettingRepository

class SaveLastWirelessAdbTargetUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(target: WirelessAdbTarget): Boolean = settingRepository.updateLastWirelessAdbTarget(target)
}
