package jp.kaleidot725.adbpad.ui.screen.setting.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierLevel
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.component.text.DefaultOutlineTextField
import jp.kaleidot725.adbpad.ui.component.text.SubTitle
import jp.kaleidot725.adbpad.ui.screen.setting.model.ScrcpyTierFieldsInput
import jp.kaleidot725.adbpad.ui.screen.setting.state.ScrcpyTierField

@Composable
fun SdkPathSettingsPane(
    initialized: Boolean,
    adbDirectoryPath: String,
    onChangeAdbDirectoryPath: (String) -> Unit,
    isValidAdbDirectoryPath: Boolean,
    adbPortNumber: String,
    onChangeAdbPortNumber: (String) -> Unit,
    isValidAdbPortNumber: Boolean,
    scrcpyBinaryPath: String,
    onChangeScrcpyBinaryPath: (String) -> Unit,
    isValidScrcpyBinaryPath: Boolean,
    lowTierPreset: ScrcpyTierFieldsInput,
    mediumTierPreset: ScrcpyTierFieldsInput,
    highTierPreset: ScrcpyTierFieldsInput,
    onChangeTierPresetField: (ScrcpyTierLevel, ScrcpyTierField, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubTitle(
            text = Language.settingAdbHeader,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        DefaultOutlineTextField(
            id = initialized,
            initialText = adbDirectoryPath,
            onUpdateText = onChangeAdbDirectoryPath,
            label = Language.settingAdbDirectoryPathTitle,
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidAdbDirectoryPath,
            placeHolder = "",
        )

        DefaultOutlineTextField(
            id = initialized,
            initialText = adbPortNumber,
            onUpdateText = onChangeAdbPortNumber,
            label = Language.settingAdbPortNumberTitle,
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidAdbPortNumber,
            placeHolder = "",
        )

        SubTitle(
            text = Language.settingScrcpyHeader,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        DefaultOutlineTextField(
            id = initialized,
            initialText = scrcpyBinaryPath,
            onUpdateText = onChangeScrcpyBinaryPath,
            label = Language.settingScrcpyBinaryPathTitle,
            modifier = Modifier.fillMaxWidth(),
            isError = !isValidScrcpyBinaryPath,
            placeHolder = "",
        )

        SubTitle(
            text = Language.settingScrcpyTierPresetsHeader,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        ScrcpyTierPresetRow(
            title = Language.settingScrcpyTierLow,
            level = ScrcpyTierLevel.LOW,
            initialized = initialized,
            preset = lowTierPreset,
            onChangeField = onChangeTierPresetField,
        )

        ScrcpyTierPresetRow(
            title = Language.settingScrcpyTierMedium,
            level = ScrcpyTierLevel.MEDIUM,
            initialized = initialized,
            preset = mediumTierPreset,
            onChangeField = onChangeTierPresetField,
        )

        ScrcpyTierPresetRow(
            title = Language.settingScrcpyTierHigh,
            level = ScrcpyTierLevel.HIGH,
            initialized = initialized,
            preset = highTierPreset,
            onChangeField = onChangeTierPresetField,
        )
    }
}

@Composable
private fun ScrcpyTierPresetRow(
    title: String,
    level: ScrcpyTierLevel,
    initialized: Boolean,
    preset: ScrcpyTierFieldsInput,
    onChangeField: (ScrcpyTierLevel, ScrcpyTierField, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DefaultOutlineTextField(
                id = initialized,
                initialText = preset.maxSize,
                onUpdateText = { onChangeField(level, ScrcpyTierField.MAX_SIZE, it) },
                label = Language.settingScrcpyTierMaxSize,
                placeHolder = Language.settingScrcpyTierMaxSizeNative,
                isError = !preset.isValid,
                modifier = Modifier.weight(1f),
            )
            DefaultOutlineTextField(
                id = initialized,
                initialText = preset.videoBitRate,
                onUpdateText = { onChangeField(level, ScrcpyTierField.VIDEO_BIT_RATE, it) },
                label = Language.settingScrcpyTierBitRate,
                placeHolder = "",
                isError = !preset.isValid,
                modifier = Modifier.weight(1f),
            )
            DefaultOutlineTextField(
                id = initialized,
                initialText = preset.maxFps,
                onUpdateText = { onChangeField(level, ScrcpyTierField.MAX_FPS, it) },
                label = Language.settingScrcpyTierFps,
                placeHolder = "",
                isError = !preset.isValid,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
