package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import jp.kaleidot725.adbpad.domain.model.command.DeviceControlCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceControlCommandRepositoryImpl(
    private val settingRepository: SettingRepository,
) : DeviceControlCommandRepository {
    private suspend fun client() =
        AndroidDebugBridgeClientFactory()
            .apply { port = settingRepository.getSdkPath().adbServerPort }
            .build()

    override suspend fun sendCommand(
        device: Device,
        command: DeviceControlCommand,
        onStart: suspend () -> Unit,
        onComplete: suspend () -> Unit,
        onFailed: suspend () -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            try {
                onStart()

                val adbClient = client()
                command.requests.forEach { request ->
                    val result = adbClient.execute(request, device.serial)
                    if (result.exitCode != 0) {
                        onFailed()
                        return@withContext
                    }
                }

                onComplete()
            } catch (e: Exception) {
                onFailed()
            }
        }
    }
}
