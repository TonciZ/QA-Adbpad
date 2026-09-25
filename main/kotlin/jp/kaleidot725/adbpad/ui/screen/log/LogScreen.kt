package jp.kaleidot725.adbpad.ui.screen.log

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.log.LogEntry
import jp.kaleidot725.adbpad.domain.model.log.LogLevel
import jp.kaleidot725.adbpad.ui.screen.log.state.LogAction
import jp.kaleidot725.adbpad.ui.screen.log.state.LogState
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LogScreen(
    state: LogState,
    onAction: (LogAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val visible = state.visibleEntries

    LaunchedEffect(visible.size, state.autoScroll) {
        if (state.autoScroll && visible.isNotEmpty()) {
            listState.scrollToItem(visible.size - 1)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxSize().padding(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.filter,
                    onValueChange = { onAction(LogAction.UpdateFilter(it)) },
                    label = { Text(Language.logFilter) },
                    singleLine = true,
                    enabled = !state.isCapturing,
                    modifier = Modifier.weight(1f),
                )

                if (!state.isCapturing) {
                    Button(onClick = { onAction(LogAction.StartCapture) }) {
                        Text(Language.logStart)
                    }
                } else {
                    Button(onClick = { onAction(LogAction.StopCapture) }) {
                        Text(Language.logStop)
                    }
                }

                OutlinedButton(onClick = { onAction(LogAction.ClearLog) }) {
                    Text(Language.logClear)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = { onAction(LogAction.UpdateSearch(it)) },
                    label = { Text(Language.logSearch) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = Language.logMinLevel,
                    style = MaterialTheme.typography.bodySmall,
                )
                LogLevel.entries.forEach { level ->
                    LevelChip(
                        level = level,
                        selected = level == state.minLevel,
                        onClick = { onAction(LogAction.UpdateMinLevel(level)) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.autoScroll,
                    onCheckedChange = { onAction(LogAction.SetAutoScroll(it)) },
                )
                Text(Language.logAutoScroll, style = MaterialTheme.typography.bodySmall)

                Checkbox(
                    checked = state.wrapLines,
                    onCheckedChange = { onAction(LogAction.SetWrapLines(it)) },
                )
                Text(Language.logWrapLines, style = MaterialTheme.typography.bodySmall)

                Box(modifier = Modifier.weight(1f))

                Text(
                    text = "${visible.size} / ${state.entries.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = { copyToClipboard(visible.joinToString("\n") { it.raw }) },
                    enabled = visible.isNotEmpty(),
                ) {
                    Text(Language.logCopyVisible)
                }
            }

            if (state.savedFile.isNotEmpty()) {
                Text(
                    text = "${Language.logSaved} ${state.savedFile}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                                // Scrolling up with the wheel pauses auto-scroll so the user can read,
                                // scrolling back to the bottom resumes it.
                                .onPointerEvent(PointerEventType.Scroll) { event ->
                                    val change = event.changes.firstOrNull()
                                    val dy = change?.scrollDelta?.y ?: 0f
                                    if (dy < 0 && state.autoScroll) {
                                        onAction(LogAction.SetAutoScroll(false))
                                    } else if (dy > 0 && !state.autoScroll && !listState.canScrollForward) {
                                        onAction(LogAction.SetAutoScroll(true))
                                    }
                                },
                    ) {
                        items(visible, key = { it.id }) { entry ->
                            LogLine(entry = entry, wrap = state.wrapLines)
                        }
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).width(8.dp).fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(scrollState = listState),
                )
            }
        }
    }
}

@Composable
private fun LevelChip(
    level: LogLevel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = levelColor(level)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(28.dp)
                .background(
                    color = if (selected) color else Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 4.dp),
    ) {
        Text(
            text = level.letter.toString(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) Color.White else color,
        )
    }
}

@Composable
private fun LogLine(
    entry: LogEntry,
    wrap: Boolean,
) {
    val level = entry.level
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val base = MaterialTheme.colorScheme.onSurfaceVariant
    val text =
        remember(entry, dim, base) {
            buildAnnotatedString {
                if (level == null) {
                    withStyle(SpanStyle(color = dim)) { append(entry.message) }
                    return@buildAnnotatedString
                }
                val color = levelColor(level)
                withStyle(SpanStyle(color = dim)) { append("${entry.time}  ") }
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append("${level.letter}  ") }
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(entry.tag) }
                withStyle(SpanStyle(color = dim)) { append(" (${entry.pid})  ") }
                val messageColor = if (level >= LogLevel.WARN) color else base
                withStyle(SpanStyle(color = messageColor)) { append(entry.message) }
            }
        }

    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        softWrap = wrap,
        maxLines = if (wrap) Int.MAX_VALUE else 1,
        overflow = if (wrap) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    )
}

// Mid-tone colors that stay readable on both light and dark surfaces.
private fun levelColor(level: LogLevel): Color =
    when (level) {
        LogLevel.VERBOSE -> Color(0xFF8A8A8A)
        LogLevel.DEBUG -> Color(0xFF4A90D9)
        LogLevel.INFO -> Color(0xFF3FA34D)
        LogLevel.WARN -> Color(0xFFD08A00)
        LogLevel.ERROR -> Color(0xFFE5484D)
        LogLevel.FATAL -> Color(0xFFB0368F)
    }

private fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
