package jp.kaleidot725.adbpad.ui.screen.setting.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.update.UpdateInfo
import jp.kaleidot725.adbpad.ui.component.text.SubTitle

@Composable
fun UpdatesSettingsPane(
    appVersion: String,
    isCheckingForUpdate: Boolean,
    updateCheckMessage: String,
    availableUpdate: UpdateInfo?,
    isInstallingUpdate: Boolean,
    onCheckForUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubTitle(
            text = Language.categoryUpdates,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Text(
            text = Language.settingCurrentVersionFormat.format(appVersion),
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedButton(
            onClick = onCheckForUpdate,
            enabled = !isCheckingForUpdate && !isInstallingUpdate,
        ) {
            if (isCheckingForUpdate) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp))
                    Text(Language.settingCheckingForUpdate)
                }
            } else {
                Text(Language.settingCheckForUpdates)
            }
        }

        if (updateCheckMessage.isNotBlank()) {
            Text(
                text = updateCheckMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (availableUpdate != null) {
            Button(
                onClick = onInstallUpdate,
                enabled = !isInstallingUpdate,
            ) {
                if (isInstallingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp))
                } else {
                    Text(Language.settingDownloadAndInstallFormat.format(availableUpdate.version))
                }
            }
        }
    }
}
