package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceState
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.ui.common.resource.clickableBackground

@Composable
fun DeviceSelector(
    selectedDevice: Device?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .widthIn(150.dp)
                    .clickableBackground(isDarker = MaterialTheme.colorScheme.surface.luminance() <= 0.5)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp),
        ) {
            if (selectedDevice != null) {
                DeviceLabel(
                    device = selectedDevice,
                    modifier = Modifier.align(Alignment.CenterVertically).weight(1.0f),
                )
            } else {
                Text(
                    text = Language.notFoundDevice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterVertically).weight(1.0f),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Device DropDown Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

/**
 * Custom name on top with the serial (IP:port for wireless devices) underneath in a smaller font.
 * Falls back to a single serial line when the device has no custom name.
 */
@Composable
fun DeviceLabel(
    device: Device,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = device.displayName,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (device.name.isNotBlank()) {
            Text(
                text = device.serial,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun SelectedDevice_Preview() {
    val sample = Device("DEVICE", "NAME", DeviceState.DEVICE)
    DeviceSelector(
        selectedDevice = sample,
        onClick = {},
    )
}
