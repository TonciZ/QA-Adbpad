package jp.kaleidot725.adbpad.ui.section.top.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceState
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.WirelessAdbTarget
import jp.kaleidot725.adbpad.ui.common.resource.UserColor
import jp.kaleidot725.adbpad.ui.section.top.state.SavedWirelessDevice

@Composable
fun DropDownDeviceMenu(
    devices: List<Device>,
    selectedDevice: Device?,
    onSelectDevice: (Device) -> Unit,
    modifier: Modifier = Modifier,
    savedDevices: List<SavedWirelessDevice> = emptyList(),
    onConnectSavedDevice: (WirelessAdbTarget) -> Unit = {},
    onEditSavedDevice: (WirelessAdbTarget) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var dropDownWidth by remember { mutableStateOf(0) }

    val savedBySerial = savedDevices.associateBy { it.target.serial }
    // Saved devices that adb does not list right now - shown so they can be reconnected or edited.
    val notConnected = savedDevices.filter { saved -> devices.none { it.serial == saved.target.serial } }

    Box(modifier) {
        DeviceSelector(
            selectedDevice = selectedDevice,
            onClick = { if (!expanded && (devices.isNotEmpty() || notConnected.isNotEmpty())) expanded = true },
            modifier =
                Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(4.dp))
                    .onSizeChanged { dropDownWidth = it.width },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier =
                Modifier
                    .widthIn(min = with(LocalDensity.current) { dropDownWidth.toDp() })
                    .background(
                        color = UserColor.getDropdownBackgroundColor(),
                        shape = RoundedCornerShape(4.dp),
                    ).border(
                        width = 1.dp,
                        color = UserColor.getSplitterColor(),
                        shape = RoundedCornerShape(4.dp),
                    ),
        ) {
            devices.forEach { device ->
                val saved = savedBySerial[device.serial]
                DropdownMenuItem(
                    text = {
                        DeviceLabel(
                            device = device,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    },
                    trailingIcon =
                        saved?.let {
                            {
                                EditDeviceButton {
                                    expanded = false
                                    onEditSavedDevice(it.target)
                                }
                            }
                        },
                    onClick = {
                        onSelectDevice(device)
                        expanded = false
                    },
                )
            }

            if (devices.isNotEmpty() && notConnected.isNotEmpty()) {
                HorizontalDivider(color = UserColor.getSplitterColor())
            }

            notConnected.forEach { saved ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                            DeviceLabel(
                                device = Device(saved.target.serial, saved.name, DeviceState.OFFLINE),
                                modifier = Modifier.alpha(0.6f),
                            )
                            Text(
                                text = Language.savedDeviceNotConnected,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    trailingIcon = {
                        EditDeviceButton {
                            expanded = false
                            onEditSavedDevice(saved.target)
                        }
                    },
                    onClick = {
                        onConnectSavedDevice(saved.target)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EditDeviceButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = Lucide.Pencil,
            contentDescription = Language.edit,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Preview
@Composable
private fun DeviceList_Preview() {
    val sample = Device("DEVICE", "NAME", DeviceState.DEVICE)
    DropDownDeviceMenu(
        devices = listOf(sample),
        selectedDevice = sample,
        onSelectDevice = {},
    )
}
