package jp.kaleidot725.adbpad.ui.section.top

import jp.kaleidot725.adbpad.domain.model.command.DeviceControlCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteDeviceControlCommandUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.ConnectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.DisconnectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.PairDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.SelectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.UpdateDevicesUseCase
import jp.kaleidot725.adbpad.domain.usecase.scrcpy.LaunchScrcpyUseCase
import jp.kaleidot725.adbpad.ui.container.AppBroadCast
import jp.kaleidot725.adbpad.ui.container.AppUnicast
import jp.kaleidot725.adbpad.ui.section.top.state.TopAction
import jp.kaleidot725.adbpad.ui.section.top.state.TopSideEffect
import jp.kaleidot725.adbpad.ui.section.top.state.TopState
import jp.kaleidot725.pulse.mvi.PulseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TopStateHolder(
    private val updateDevicesUseCase: UpdateDevicesUseCase,
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val selectDeviceUseCase: SelectDeviceUseCase,
    private val executeDeviceControlCommandUseCase: ExecuteDeviceControlCommandUseCase,
    private val launchScrcpyUseCase: LaunchScrcpyUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
) : PulseStore<TopState, TopAction, TopSideEffect, AppBroadCast, AppUnicast>(TopState()) {
    private var deviceJob: Job? = null
    private var selectedDeviceJob: Job? = null

    override fun onSetup() {
        collectDevices()
    }

    override fun onAction(uiAction: TopAction) {
        coroutineScope.launch {
            when (uiAction) {
                is TopAction.ExecuteCommand -> executeCommand(uiAction.command)
                is TopAction.SelectDevice -> selectDevice(uiAction.device)
                TopAction.LaunchScrcpy -> launchScrcpy()
                TopAction.Refresh -> unicast(AppUnicast.Refresh)
                TopAction.OpenWirelessAdb -> update { copy(showWirelessAdbDialog = true, wirelessAdbStatus = "") }
                TopAction.CloseWirelessAdb -> update { copy(showWirelessAdbDialog = false, wirelessAdbStatus = "", wirelessAdbLoading = false) }
                is TopAction.ConnectWirelessAdb -> connectWirelessAdb(uiAction.host, uiAction.port)
                is TopAction.PairWirelessAdb -> pairWirelessAdb(uiAction.host, uiAction.port, uiAction.code)
                is TopAction.DisconnectWirelessAdb -> disconnectWirelessAdb(uiAction.host, uiAction.port)
            }
        }
    }

    override fun onReceive(broadcast: AppBroadCast) {
        when (broadcast) {
            AppBroadCast.Refresh -> collectDevices()
        }
    }

    private suspend fun selectDevice(device: Device) {
        selectDeviceUseCase(device)
    }

    private suspend fun executeCommand(command: DeviceControlCommand) {
        val device = currentState.selectedDevice ?: return
        executeDeviceControlCommandUseCase(
            device = device,
            command = command,
        )
    }

    private suspend fun launchScrcpy() {
        val device = currentState.selectedDevice ?: return
        try {
            launchScrcpyUseCase(device)
        } catch (e: Exception) {
            println("Failed to launch Scrcpy: ${e.message}")
        }
    }

    private suspend fun connectWirelessAdb(host: String, port: Int) {
        update { copy(wirelessAdbLoading = true, wirelessAdbStatus = "") }
        try {
            val result = connectDeviceUseCase(host, port)
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = result) }
        } catch (e: Exception) {
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = "Error: ${e.message}") }
        }
    }

    private suspend fun pairWirelessAdb(host: String, port: Int, code: String) {
        update { copy(wirelessAdbLoading = true, wirelessAdbStatus = "") }
        try {
            val result = pairDeviceUseCase(host, port, code)
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = result) }
        } catch (e: Exception) {
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = "Error: ${e.message}") }
        }
    }

    private suspend fun disconnectWirelessAdb(host: String, port: Int) {
        update { copy(wirelessAdbLoading = true, wirelessAdbStatus = "") }
        try {
            val result = disconnectDeviceUseCase(host, port)
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = result) }
        } catch (e: Exception) {
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = "Error: ${e.message}") }
        }
    }

    private fun collectDevices() {
        deviceJob?.cancel()
        deviceJob =
            coroutineScope.launch {
                while (isActive) {
                    val devices = updateDevicesUseCase()
                    update { this.copy(devices = devices) }
                    delay(1000)
                }
            }

        selectedDeviceJob?.cancel()
        selectedDeviceJob =
            coroutineScope.launch {
                getSelectedDeviceFlowUseCase().collect {
                    update { this.copy(selectedDevice = it) }
                }
            }
    }
}
