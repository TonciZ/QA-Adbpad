package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness
import jp.kaleidot725.adbpad.domain.model.device.DeviceState

@Composable
fun DeviceStatusDot(
    state: DeviceState,
    liveness: DeviceLiveness? = null,
    modifier: Modifier = Modifier,
) {
    val color =
        when {
            state != DeviceState.DEVICE -> Color(0xFFF44336) // red - adb link down
            liveness == DeviceLiveness.UNRESPONSIVE -> Color(0xFFFFC107) // yellow - connected but frozen
            liveness == DeviceLiveness.RESPONSIVE -> Color(0xFF4CAF50) // green
            liveness == DeviceLiveness.CHECKING || liveness == DeviceLiveness.UNKNOWN || liveness == null -> Color(0xFF9E9E9E) // gray
            else -> Color(0xFF9E9E9E)
        }
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
    )
}
