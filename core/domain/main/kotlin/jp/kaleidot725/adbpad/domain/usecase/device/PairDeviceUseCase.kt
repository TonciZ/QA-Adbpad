package jp.kaleidot725.adbpad.domain.usecase.device

import jp.kaleidot725.adbpad.domain.repository.DeviceConnectionRepository

class PairDeviceUseCase(
    private val deviceConnectionRepository: DeviceConnectionRepository,
) {
    suspend operator fun invoke(host: String, port: Int, code: String): String {
        return deviceConnectionRepository.pairDevice(host, port, code)
    }
}
