package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.component.button.FloatingDialog

@Composable
fun ScrcpyErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    FloatingDialog(onDismiss = onDismiss, modifier = Modifier.width(420.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = Language.scrcpyErrorTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            // Selectable so a tester can copy the reason into a message.
            SelectionContainer {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(Language.close)
            }
        }
    }
}
