package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.repository.DeviceHealthRepository

class RestartDeviceUseCase(
    private val deviceHealthRepository: DeviceHealthRepository,
) {
    suspend operator fun invoke(device: Device) = deviceHealthRepository.restart(device)
}
