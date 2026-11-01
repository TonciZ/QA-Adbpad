package jp.kaleidot725.adbpad.domain.repository

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Each client owns a Vert.x instance (thread pools) released only by close(); cache and
// reuse one per port instead of building a new one on every call.
class AdbClientProvider(
    private val settingRepository: SettingRepository,
) {
    private val mutex = Mutex()
    private var cachedPort: Int? = null
    private var cachedClient: AndroidDebugBridgeClient? = null

    suspend fun client(): AndroidDebugBridgeClient =
        mutex.withLock {
            val port = settingRepository.getSdkPath().adbServerPort
            val existing = cachedClient
            if (existing != null && cachedPort == port) return existing

            existing?.close()
            val client = AndroidDebugBridgeClientFactory().apply { this.port = port }.build()
            cachedPort = port
            cachedClient = client
            client
        }
}
