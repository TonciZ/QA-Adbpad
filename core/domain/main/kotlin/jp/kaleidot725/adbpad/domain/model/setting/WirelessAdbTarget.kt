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

        /** Accepts `192.168.1.5`, `192.168.1.5:5555` and `adb connect 192.168.1.5:5555`; a port in the text wins. */
        fun parse(
            hostText: String,
            port: Int,
        ): WirelessAdbTarget {
            val text = hostText.trim().removePrefix("adb connect").trim()
            val host = text.substringBeforeLast(':')
            val textPort = text.substringAfterLast(':', "").toIntOrNull()
            return if (text.count { it == ':' } == 1 && textPort != null) {
                WirelessAdbTarget(host.trim(), textPort)
            } else {
                WirelessAdbTarget(text, port)
            }
        }
    }
}
