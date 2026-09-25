package jp.kaleidot725.adbpad.domain.model.setting

import kotlinx.serialization.Serializable

@Serializable
data class WirelessAdbTarget(
    val host: String = "",
    val port: Int = DEFAULT_PORT,
) {
    val serial: String get() = "$host:$port"

    companion object {
        const val DEFAULT_PORT = 5555
    }
}
