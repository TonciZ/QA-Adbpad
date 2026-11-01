package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import com.malinskiy.adam.request.misc.PairDeviceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceConnectionRepositoryImpl(
    private val adbClientProvider: AdbClientProvider,
) : DeviceConnectionRepository {
    private suspend fun client() = adbClientProvider.client()

    override suspend fun connectDevice(
        host: String,
        port: Int,
    ): String =
        withContext(Dispatchers.IO) {
            client().execute(ConnectDeviceRequest(host, port))
        }

    override suspend fun pairDevice(
        host: String,
        port: Int,
        code: String,
    ): String =
        withContext(Dispatchers.IO) {
            client().execute(PairDeviceRequest("$host:$port", code))
        }

    override suspend fun disconnectDevice(
        host: String,
        port: Int,
    ): String =
        withContext(Dispatchers.IO) {
            client().execute(DisconnectDeviceRequest(host, port))
        }
}
