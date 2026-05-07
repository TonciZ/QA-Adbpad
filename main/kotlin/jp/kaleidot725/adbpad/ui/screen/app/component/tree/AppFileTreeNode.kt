package jp.kaleidot725.adbpad.ui.screen.app.component.tree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import jp.kaleidot725.adbpad.domain.model.app.AppFileEntry
import jp.kaleidot725.adbpad.ui.common.resource.clickableBackground

@Composable
internal fun AppFileTreeNode(
    entry: AppFileEntry,
    selectedFile: AppFileEntry?,
    onSelectNode: (AppFileEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedFile?.path == entry.path

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickableBackground(
                    isSelected = isSelected,
                    shape = RoundedCornerShape(4.dp),
                ).clickable { onSelectNode(entry) }
                .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector =
                if (entry.isDirectory) {
                    Lucide.Folder
                } else {
                    Lucide.File
                },
            contentDescription = null,
            tint =
                if (entry.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
private fun AppFileTreeNodePreview() {
    AppFileTreeNode(
        entry = previewAppFileEntries.first(),
        selectedFile = previewAppFileEntries.first(),
        onSelectNode = {},
        modifier = Modifier.width(280.dp).padding(16.dp),
    )
}

