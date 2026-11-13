package jp.kaleidot725.adbpad.ui.screen.command.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.command.CommandExecutionHistory
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteCustomCommandUseCase

@Composable
fun CommandOutput(
    executionHistory: CommandExecutionHistory?,
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    onRun: (String) -> Unit = {},
    onStop: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Text(
            text = Language.outputTerminalTitle,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )

        CustomCommandInput(
            isRunning = isRunning,
            onRun = onRun,
            onStop = onStop,
        )

        // Content
        if (executionHistory == null) {
            Text(
                text = Language.outputTerminalPlaceholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            val scrollState = rememberScrollState()
            // Follow the tail while a custom command is still printing.
            LaunchedEffect(scrollState.maxValue, isRunning) {
                if (isRunning) scrollState.scrollTo(scrollState.maxValue)
            }

            SelectionContainer {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Command
                    Text(
                        text = executionHistory.command,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Output
                    if (executionHistory.output.isNotEmpty()) {
                        Text(
                            text = highlightPrompts(executionHistory.output, MaterialTheme.colorScheme.primary),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomCommandInput(
    isRunning: Boolean,
    onRun: (String) -> Unit,
    onStop: () -> Unit,
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = { Text(Language.customCommandPlaceholder, style = textStyle, maxLines = 1) },
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            maxLines = 4,
            modifier =
                Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                        when {
                            !isEnter || event.type != KeyEventType.KeyDown -> false

                            // Shift+Enter: a new line, so several commands can be queued up.
                            event.isShiftPressed -> {
                                input = input.insertAtCursor("\n")
                                true
                            }

                            else -> {
                                if (!isRunning) onRun(input.text)
                                true
                            }
                        }
                    },
        )

        if (isRunning) {
            OutlinedButton(onClick = onStop) { Text(Language.customCommandStop) }
        } else {
            Button(
                onClick = { onRun(input.text) },
                enabled = input.text.isNotBlank(),
            ) { Text(Language.execute) }
        }
    }
}

private fun TextFieldValue.insertAtCursor(insert: String): TextFieldValue {
    val newText = text.replaceRange(selection.min, selection.max, insert)
    return copy(text = newText, selection = TextRange(selection.min + insert.length))
}

/** Colors the `$ adb ...` lines of a multi-command transcript like the first command line. */
private fun highlightPrompts(
    output: String,
    promptColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        output.lineSequence().forEachIndexed { index, line ->
            if (index > 0) append('\n')
            if (line.startsWith(ExecuteCustomCommandUseCase.PROMPT)) {
                withStyle(SpanStyle(color = promptColor)) { append(line) }
            } else {
                append(line)
            }
        }
    }
