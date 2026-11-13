package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.domain.repository.SettingRepository

class GetSavedWirelessDevicesUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(): List<WirelessAdbTarget> = settingRepository.getSavedWirelessDevices()
}
