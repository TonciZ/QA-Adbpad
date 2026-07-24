package jp.kaleidot725.adbpad.ui.section.top.state

import jp.kaleidot725.pulse.mvi.PulseState
import jp.kaleidot725.adbpad.domain.model.device.Device

data class TopState(
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val showWirelessAdbDialog: Boolean = false,
    val wirelessAdbStatus: String = "",
    val wirelessAdbLoading: Boolean = false,
) : PulseState
