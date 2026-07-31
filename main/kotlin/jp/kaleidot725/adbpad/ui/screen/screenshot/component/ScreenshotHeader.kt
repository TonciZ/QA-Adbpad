package jp.kaleidot725.adbpad.ui.screen.screenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.sort.SortType
import jp.kaleidot725.adbpad.ui.component.dropbox.SearchSortDropBox
import jp.kaleidot725.adbpad.ui.component.text.DefaultTextField

@Composable
fun ScreenshotHeader(
    searchText: String,
    sortType: SortType,
    onUpdateSortType: (SortType) -> Unit,
    onUpdateSearchText: (String) -> Unit,
    canCapture: Boolean,
    isCapturing: Boolean,
    onTakeScreenshot: () -> Unit,
    totalCount: Int,
    selectedCount: Int,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = totalCount > 0 && selectedCount == totalCount,
            onCheckedChange = { onToggleSelectAll() },
            enabled = totalCount > 0,
        )

        SearchSortDropBox(
            selectedSortType = sortType,
            onSelectType = onUpdateSortType,
        )

        DefaultTextField(
            initialText = searchText,
            onUpdateText = onUpdateSearchText,
            placeHolder = Language.search,
            modifier = Modifier.weight(1.0f),
        )

        if (selectedCount > 0) {
            IconButton(
                onClick = onDeleteSelected,
                modifier =
                    Modifier
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                        .size(32.dp),
            ) {
                Icon(
                    imageVector = Lucide.Trash2,
                    contentDescription = "delete selected screenshots",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        IconButton(
            onClick = onTakeScreenshot,
            enabled = canCapture && !isCapturing,
            modifier =
                Modifier
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .size(32.dp)
                    .alpha(if (canCapture) 1f else 0.38f),
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Icon(
                    imageVector = Lucide.Camera,
                    contentDescription = "capture screenshot",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ScreenshotHeader(
        searchText = "TEST",
        sortType = SortType.SORT_BY_NAME_ASC,
        onUpdateSortType = {},
        onUpdateSearchText = {},
        canCapture = false,
        isCapturing = false,
        onTakeScreenshot = {},
        totalCount = 0,
        selectedCount = 0,
        onToggleSelectAll = {},
        onDeleteSelected = {},
    )
}
