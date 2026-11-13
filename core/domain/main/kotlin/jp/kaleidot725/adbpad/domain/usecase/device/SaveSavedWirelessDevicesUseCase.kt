package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.domain.repository.SettingRepository

class SaveSavedWirelessDevicesUseCase(
    private val settingRepository: SettingRepository,
) {
    suspend operator fun invoke(devices: List<WirelessAdbTarget>): Boolean = settingRepository.updateSavedWirelessDevices(devices)
}
