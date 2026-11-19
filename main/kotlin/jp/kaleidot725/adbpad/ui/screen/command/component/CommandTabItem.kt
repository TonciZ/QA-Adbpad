package jp.kaleidot725.adbpad.ui.screen.command.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Presentation
import jp.kaleidot725.adbpad.ui.common.resource.UserColor
import jp.kaleidot725.adbpad.ui.common.resource.clickableBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // False when the tab row is short of space: the tab shows its icon only, the title moves
    // to a tooltip and to the icon's description.
    showLabel: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Box(
            modifier
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickableBackground(isSelected = isSelected, shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (showLabel) null else title,
                    modifier = Modifier.size(16.dp),
                )

                if (showLabel) {
                    val textStyle =
                        if (isSelected) {
                            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal)
                        }

                    Text(
                        text = title,
                        textAlign = TextAlign.Center,
                        style = textStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    if (showLabel) {
        content()
    } else {
        TooltipArea(
            delayMillis = 300,
            tooltip = {
                Card(modifier = Modifier.border(1.dp, UserColor.getSplitterColor())) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    )
                }
            },
            tooltipPlacement =
                TooltipPlacement.ComponentRect(
                    anchor = Alignment.BottomCenter,
                    alignment = Alignment.BottomCenter,
                    offset = DpOffset(0.dp, 4.dp),
                ),
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun CommandTabItemPreview() {
    Row {
        CommandTabItem("one", Lucide.Presentation, false, {})
        CommandTabItem("two", Lucide.Presentation, true, {})
    }
}
