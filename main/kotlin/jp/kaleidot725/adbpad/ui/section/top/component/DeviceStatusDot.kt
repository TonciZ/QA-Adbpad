package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness
import jp.kaleidot725.adbpad.domain.model.device.DeviceState

@Composable
fun DeviceStatusDot(
    state: DeviceState,
    liveness: DeviceLiveness? = null,
    modifier: Modifier = Modifier,
) {
    // The dot is the only status indicator here, so each colour keeps at least 3:1 against the
    // toolbar in its theme (WCAG 1.4.11). The bright set used on dark fails on a light toolbar.
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val red = if (isLight) Color(0xFFC62828) else Color(0xFFF44336)
    val amber = if (isLight) Color(0xFF8A5A00) else Color(0xFFFFC107)
    val green = if (isLight) Color(0xFF2E7D32) else Color(0xFF4CAF50)
    val gray = if (isLight) Color(0xFF6E6E6E) else Color(0xFF9E9E9E)
    val color =
        when {
            state != DeviceState.DEVICE -> red // adb link down
            liveness == DeviceLiveness.UNRESPONSIVE -> amber // connected but frozen
            liveness == DeviceLiveness.RESPONSIVE -> green
            else -> gray // checking or unknown
        }
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
    )
}
