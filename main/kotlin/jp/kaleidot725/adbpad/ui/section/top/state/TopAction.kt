package jp.kaleidot725.adbpad.ui.section.top.state

import jp.kaleidot725.adbpad.domain.model.command.DeviceControlCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierLevel
import jp.kaleidot725.pulse.mvi.PulseAction

sealed class TopAction : PulseAction {
    data class SelectDevice(
        val device: Device,
    ) : TopAction()

    data class ExecuteCommand(
        val command: DeviceControlCommand,
    ) : TopAction()

    data object LaunchScrcpy : TopAction()

    data object OpenScrcpyTierDialog : TopAction()

    data object CloseScrcpyTierDialog : TopAction()

    data class LaunchScrcpyWithTier(
        val level: ScrcpyTierLevel,
    ) : TopAction()

    data object ScanDeviceProfile : TopAction()

    data object LaunchScrcpyWithProfile : TopAction()

    data object CheckDeviceLiveness : TopAction()

    data object RestartDevice : TopAction()

    data object Refresh : TopAction()

    data object OpenWirelessAdb : TopAction()

    data object CloseWirelessAdb : TopAction()

    data class ConnectWirelessAdb(
        val host: String,
        val port: Int,
    ) : TopAction()

    data class PairWirelessAdb(
        val host: String,
        val port: Int,
        val code: String,
    ) : TopAction()

    data class DisconnectWirelessAdb(
        val host: String,
        val port: Int,
    ) : TopAction()
}
