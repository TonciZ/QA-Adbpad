package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.repository.DeviceConnectionRepository

class DisconnectDeviceUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository,
) {
    suspend operator fun invoke(host: String, port: Int): String {
        return deviceConnectionRepository.disconnectDevice(host, port)
    }
}
