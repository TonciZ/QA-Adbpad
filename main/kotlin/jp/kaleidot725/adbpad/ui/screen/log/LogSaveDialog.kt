package jp.kaleidot725.adbpad.ui.screen.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.component.button.FloatingDialog
import jp.kaleidot725.adbpad.ui.screen.log.state.LogAction
import jp.kaleidot725.adbpad.ui.screen.log.state.LogSaveDialogState

@Composable
fun LogSaveDialog(
    dialog: LogSaveDialogState,
    onAction: (LogAction) -> Unit,
) {
    FloatingDialog(onDismiss = { onAction(LogAction.CloseSaveDialog) }, modifier = Modifier.width(520.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = Language.logSaveTitle, style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = dialog.fileName,
                onValueChange = { onAction(LogAction.UpdateSaveFileName(it)) },
                label = { Text(Language.logSaveFileName) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Language.logSaveFolder,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = dialog.directory,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(onClick = { onAction(LogAction.ChooseSaveDirectory) }) {
                    Text(Language.logSaveChangeFolder)
                }
            }

            if (dialog.canSaveShownOnly) {
                Column {
                    SaveScopeOption(
                        text = Language.logSaveAllFormat.format(dialog.totalLines),
                        selected = !dialog.onlyShown,
                        onClick = { onAction(LogAction.SetSaveOnlyShown(false)) },
                    )
                    SaveScopeOption(
                        text = Language.logSaveShownFormat.format(dialog.shownLines),
                        selected = dialog.onlyShown,
                        onClick = { onAction(LogAction.SetSaveOnlyShown(true)) },
                    )
                }
            } else {
                Text(
                    text = Language.logSaveAllFormat.format(dialog.totalLines),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (dialog.failed) {
                Text(
                    text = Language.logSaveFailed,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { onAction(LogAction.CloseSaveDialog) }) {
                    Text(Language.cancel)
                }
                Button(
                    onClick = { onAction(LogAction.ConfirmSave) },
                    enabled = dialog.fileName.isNotBlank(),
                ) {
                    Text(Language.save)
                }
            }
        }
    }
}

@Composable
private fun SaveScopeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}
