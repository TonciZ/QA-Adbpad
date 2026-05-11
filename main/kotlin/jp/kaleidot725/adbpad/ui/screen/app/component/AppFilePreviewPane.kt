package jp.kaleidot725.adbpad.ui.screen.app.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Save
import jp.kaleidot725.adbpad.domain.model.app.AppFileEntry
import jp.kaleidot725.adbpad.domain.model.app.AppFilePreview
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.common.resource.UserColor
import jp.kaleidot725.adbpad.ui.component.button.CommandIconButton
import jp.kaleidot725.adbpad.ui.component.button.CommandTextButton
import jp.kaleidot725.adbpad.ui.screen.app.state.AppFilePreviewState
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun AppFilePreviewPane(
    state: AppFilePreviewState,
    onSaveFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AppFilePreviewHeader(
            entry = state.entry,
            canSave = state.entry is AppFileEntry.File && !state.isLoading && !state.isSaving,
            onSaveFile = onSaveFile,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        HorizontalDivider(color = UserColor.getSplitterColor())

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null -> {
                    AppFilePreviewNoImage(
                        details = state.errorMessage,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
                state.preview is AppFilePreview.Image -> {
                    AppFileImagePreview(
                        preview = state.preview,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                state.preview is AppFilePreview.Text -> {
                    AppFileTextPreview(
                        text = state.preview.text,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                    )
                }
                else -> {
                    AppFilePreviewNoImage(modifier = Modifier.align(Alignment.Center).padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AppFilePreviewHeader(
    entry: AppFileEntry?,
    canSave: Boolean,
    onSaveFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = Language.appFilePreviewTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            CommandIconButton(
                image = Lucide.Save,
                onClick = onSaveFile,
                enabled = canSave,
                modifier = Modifier.size(32.dp),
            )
        }
        if (entry != null) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppFileImagePreview(
    preview: AppFilePreview.Image,
    modifier: Modifier = Modifier,
) {
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier) {
        val constraints = this

        AsyncImage(
            model = preview.localFile,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RectangleShape)
                    .zoomable(zoomState),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(
                        color = UserColor.getFloatingBackgroundColor(),
                        shape = RoundedCornerShape(4.dp),
                    ),
        ) {
            CommandIconButton(
                image = Lucide.Plus,
                onClick = {
                    coroutineScope.launch {
                        zoomState.changeScale(
                            targetScale = zoomState.scale + 0.5f,
                            position =
                                Offset(
                                    constraints.maxWidth.value / 2f,
                                    constraints.maxHeight.value / 2f,
                                ),
                            animationSpec = tween(),
                        )
                    }
                },
                modifier =
                    Modifier
                        .padding(4.dp)
                        .size(32.dp),
            )

            CommandIconButton(
                image = Lucide.Minus,
                onClick = {
                    coroutineScope.launch {
                        zoomState.changeScale(
                            targetScale = zoomState.scale - 0.5f,
                            position =
                                Offset(
                                    constraints.maxWidth.value / 2f,
                                    constraints.maxHeight.value / 2f,
                                ),
                            animationSpec = tween(),
                        )
                    }
                },
                modifier =
                    Modifier
                        .padding(4.dp)
                        .size(32.dp),
            )

            CommandTextButton(
                text = "100%",
                onClick = {
                    coroutineScope.launch {
                        zoomState.reset()
                    }
                },
                modifier =
                    Modifier
                        .padding(4.dp)
                        .size(32.dp),
            )
        }
    }
}

@Composable
private fun AppFileTextPreview(
    text: String,
    modifier: Modifier = Modifier,
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier =
                modifier
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState),
        )
    }
}

@Composable
private fun AppFilePreviewNoImage(
    details: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = Language.appFilePreviewNoImage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!details.isNullOrBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun AppFilePreviewPanePreview() {
    AppFilePreviewPane(
        state =
            AppFilePreviewState(
                entry =
                    AppFileEntry.File(
                        name = "settings.json",
                        path = "/data/data/com.example/files/settings.json",
                        permissions = "-rw-r--r--",
                        size = 128,
                        date = "2026-05-10",
                        time = "12:00",
                    ),
                preview =
                    AppFilePreview.Text(
                        entry =
                            AppFileEntry.File(
                                name = "settings.json",
                                path = "/data/data/com.example/files/settings.json",
                                permissions = "-rw-r--r--",
                                size = 128,
                                date = "2026-05-10",
                                time = "12:00",
                            ),
                        text = "{\n  \"enabled\": true\n}",
                    ),
            ),
        onSaveFile = {},
        modifier = Modifier.fillMaxSize(),
    )
}
