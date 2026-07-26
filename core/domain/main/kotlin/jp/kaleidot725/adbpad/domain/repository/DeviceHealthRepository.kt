package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness

interface DeviceHealthRepository {
    suspend fun checkLiveness(device: Device): DeviceLiveness

    suspend fun restart(device: Device)
}
