package jp.kaleidot725.adbpad.domain.repository

interface DeviceConnectionRepository {
    suspend fun connectDevice(host: String, port: Int): String
    suspend fun pairDevice(host: String, port: Int, code: String): String
    suspend fun disconnectDevice(host: String, port: Int): String
}
