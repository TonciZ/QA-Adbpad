package jp.kaleidot725.adbpad.ui.screen.command.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Clock3
import com.composables.icons.lucide.Diamond
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Navigation
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Wifi
import jp.kaleidot725.adbpad.domain.model.command.NormalCommandCategory
import jp.kaleidot725.adbpad.domain.model.language.Language
import kotlinx.coroutines.launch

private const val WHEEL_STEP = 60f
private val FULL_LABELS_MIN_WIDTH = 660.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommandTab(
    modifier: Modifier = Modifier,
    filtered: NormalCommandCategory,
    onClick: (NormalCommandCategory) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val tabs =
        listOf(
            CommandTabEntry(Language.commandCategoryFavorite, Icons.Filled.PushPin, NormalCommandCategory.FAVORITE),
            CommandTabEntry(Language.commandCategoryAll, Lucide.Diamond, NormalCommandCategory.ALL),
            CommandTabEntry(Language.commandCategoryCommunication, Lucide.Wifi, NormalCommandCategory.COM),
            CommandTabEntry(Language.commandCategoryNavigation, Lucide.Navigation, NormalCommandCategory.NAVIGATION),
            CommandTabEntry(Language.commandCategoryTheme, Lucide.Palette, NormalCommandCategory.THEME),
            CommandTabEntry(Language.commandCategoryDisplay, Lucide.Monitor, NormalCommandCategory.DISPLAY),
            CommandTabEntry(Language.commandCategoryDevice, Lucide.Smartphone, NormalCommandCategory.DEVICE),
            CommandTabEntry(Language.commandCategoryTime, Lucide.Clock3, NormalCommandCategory.TIME),
        )
    BoxWithConstraints(modifier = modifier) {
        // All eight labels need about 640dp. Below that only the selected tab keeps its label,
        // so the row fits without scrolling down to very narrow windows.
        val compact = maxWidth < FULL_LABELS_MIN_WIDTH
        Row(
            modifier =
                Modifier
                    .horizontalScroll(scrollState)
                    // A mouse wheel only scrolls vertically; without this the tabs past the edge
                    // could be reached only with Shift+wheel or a touchpad.
                    .onPointerEvent(PointerEventType.Scroll) { event ->
                        val delta =
                            event.changes
                                .firstOrNull()
                                ?.scrollDelta
                                ?.y ?: 0f
                        if (delta != 0f) scope.launch { scrollState.scrollBy(delta * WHEEL_STEP) }
                    },
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = filtered == tab.category
                CommandTabItem(
                    title = tab.title,
                    icon = tab.icon,
                    isSelected = isSelected,
                    onClick = { onClick(tab.category) },
                    showLabel = !compact || isSelected,
                )
            }
        }
    }
}

private data class CommandTabEntry(
    val title: String,
    val icon: ImageVector,
    val category: NormalCommandCategory,
)

@Preview
@Composable
private fun CommandTabPreview() {
    CommandTab(filtered = NormalCommandCategory.ALL) {}
}
