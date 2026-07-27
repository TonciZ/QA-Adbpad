package jp.kaleidot725.adbpad.ui.screen.setting.model

import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPreset

data class ScrcpyTierFieldsInput(
    val maxSize: String = "",
    val videoBitRate: String = "",
    val maxFps: String = "",
) {
    val isValid: Boolean
        get() = (maxSize.isBlank() || maxSize.toIntOrNull() != null) && videoBitRate.toIntOrNull() != null && maxFps.toIntOrNull() != null

    fun toPreset(): ScrcpyTierPreset =
        ScrcpyTierPreset(
            maxSize = maxSize.toIntOrNull(),
            videoBitRate = videoBitRate.toIntOrNull() ?: 0,
            maxFps = maxFps.toIntOrNull() ?: 0,
        )

    companion object {
        fun from(preset: ScrcpyTierPreset): ScrcpyTierFieldsInput =
            ScrcpyTierFieldsInput(
                maxSize = preset.maxSize?.toString() ?: "",
                videoBitRate = preset.videoBitRate.toString(),
                maxFps = preset.maxFps.toString(),
            )
    }
}
