package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import com.malinskiy.adam.request.misc.PairDeviceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceConnectionRepositoryImpl : DeviceConnectionRepository {
    private val adbClient = AndroidDebugBridgeClientFactory().build()

    override suspend fun connectDevice(host: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            adbClient.execute(ConnectDeviceRequest(host, port))
        }
    }

    override suspend fun pairDevice(host: String, port: Int, code: String): String {
        return withContext(Dispatchers.IO) {
            adbClient.execute(PairDeviceRequest("$host:$port", code))
        }
    }

    override suspend fun disconnectDevice(host: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            adbClient.execute(DisconnectDeviceRequest(host, port))
        }
    }
}
