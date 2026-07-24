package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.repository.DeviceConnectionRepository

class ConnectDeviceUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository,
) {
    suspend operator fun invoke(host: String, port: Int): String {
        return deviceConnectionRepository.connectDevice(host, port)
    }
}
