package jp.kaleidot725.adbpad.domain.repository

import jp.kaleidot725.adbpad.domain.model.command.DeviceControlCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceControlCommandRepositoryImpl(
    private val adbClientProvider: AdbClientProvider,
) : DeviceControlCommandRepository {
    private suspend fun client() = adbClientProvider.client()

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
