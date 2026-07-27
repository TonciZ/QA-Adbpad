package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.misc.ConnectDeviceRequest
import com.malinskiy.adam.request.misc.DisconnectDeviceRequest
import com.malinskiy.adam.request.misc.PairDeviceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceConnectionRepositoryImpl(
    private val settingRepository: SettingRepository,
) : DeviceConnectionRepository {
    // ponytail: built per-call, not cached - port comes from user settings and can change
    // without an app restart; construction itself doesn't open a connection.
    private suspend fun client() =
        AndroidDebugBridgeClientFactory()
            .apply { port = settingRepository.getSdkPath().adbServerPort }
            .build()

    override suspend fun connectDevice(host: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            client().execute(ConnectDeviceRequest(host, port))
        }
    }

    override suspend fun pairDevice(host: String, port: Int, code: String): String {
        return withContext(Dispatchers.IO) {
            client().execute(PairDeviceRequest("$host:$port", code))
        }
    }

    override suspend fun disconnectDevice(host: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            client().execute(DisconnectDeviceRequest(host, port))
        }
    }
}
