package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierLevel
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPreset
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.component.button.FloatingDialog

@Composable
fun ScrcpyTierDialog(
    presets: ScrcpyTierPresets,
    onSelectTier: (ScrcpyTierLevel) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    FloatingDialog(onDismiss = onDismiss, modifier = Modifier.width(360.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = Language.scrcpyTierDialogTitle,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = Language.scrcpyTierDialogSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TierButton(
                title = Language.settingScrcpyTierLow,
                preset = presets.low,
                onClick = { onSelectTier(ScrcpyTierLevel.LOW) },
            )

            TierButton(
                title = Language.settingScrcpyTierMedium,
                preset = presets.medium,
                onClick = { onSelectTier(ScrcpyTierLevel.MEDIUM) },
            )

            TierButton(
                title = Language.settingScrcpyTierHigh,
                preset = presets.high,
                onClick = { onSelectTier(ScrcpyTierLevel.HIGH) },
            )

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Language.scrcpyTierDialogSkip)
            }
        }
    }
}

@Composable
private fun TierButton(
    title: String,
    preset: ScrcpyTierPreset,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            val size = preset.maxSize?.let { "${it}p" } ?: Language.settingScrcpyTierMaxSizeNative
            Text(
                text = "$size, ${preset.videoBitRate / 1_000_000}Mbps, ${preset.maxFps}fps",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
