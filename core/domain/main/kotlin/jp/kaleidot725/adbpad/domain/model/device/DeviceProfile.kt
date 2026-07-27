package jp.kaleidot725.adbpad.domain.model.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProfile(
    val videoEncoder: String,
    val safeBitRate: Int,
    val safeMaxFps: Int,
    val settleDelayMs: Long,
) {
    fun toTierPreset(): ScrcpyTierPreset =
        ScrcpyTierPreset(
            maxSize = null,
            videoBitRate = safeBitRate,
            maxFps = safeMaxFps,
        )
}
