package jp.kaleidot725.adbpad.domain.model.device

import kotlinx.serialization.Serializable

enum class ScrcpyTierLevel {
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
data class ScrcpyTierPreset(
    val maxSize: Int? = null,
    val videoBitRate: Int = 0,
    val maxFps: Int = 0,
) {
    fun applyTo(options: ScrcpyOptions): ScrcpyOptions =
        options.copy(
            maxSize = maxSize,
            videoBitRate = videoBitRate,
            maxFps = maxFps,
        )
}

@Serializable
data class ScrcpyTierPresets(
    val low: ScrcpyTierPreset = ScrcpyTierPreset(maxSize = 720, videoBitRate = 2_000_000, maxFps = 24),
    val medium: ScrcpyTierPreset = ScrcpyTierPreset(maxSize = 1080, videoBitRate = 6_000_000, maxFps = 30),
    val high: ScrcpyTierPreset = ScrcpyTierPreset(maxSize = null, videoBitRate = 12_000_000, maxFps = 60),
) {
    fun get(level: ScrcpyTierLevel): ScrcpyTierPreset =
        when (level) {
            ScrcpyTierLevel.LOW -> low
            ScrcpyTierLevel.MEDIUM -> medium
            ScrcpyTierLevel.HIGH -> high
        }

    fun with(
        level: ScrcpyTierLevel,
        preset: ScrcpyTierPreset,
    ): ScrcpyTierPresets =
        when (level) {
            ScrcpyTierLevel.LOW -> copy(low = preset)
            ScrcpyTierLevel.MEDIUM -> copy(medium = preset)
            ScrcpyTierLevel.HIGH -> copy(high = preset)
        }
}
