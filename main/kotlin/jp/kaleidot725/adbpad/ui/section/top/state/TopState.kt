package jp.kaleidot725.adbpad.ui.section.top.state

import jp.kaleidot725.pulse.mvi.PulseState
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPresets

data class TopState(
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val deviceLiveness: DeviceLiveness = DeviceLiveness.UNKNOWN,
    val showWirelessAdbDialog: Boolean = false,
    val wirelessAdbStatus: String = "",
    val wirelessAdbLoading: Boolean = false,
    val showScrcpyTierDialog: Boolean = false,
    val scrcpyTierPresets: ScrcpyTierPresets = ScrcpyTierPresets(),
) : PulseState
