package jp.kaleidot725.adbpad.ui.section.top

import jp.kaleidot725.adbpad.domain.model.command.DeviceControlCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceLiveness
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierLevel
import jp.kaleidot725.adbpad.domain.repository.DeviceSettingsRepository
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteDeviceControlCommandUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.CheckDeviceLivenessUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.ConnectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.DisconnectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.PairDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.RestartDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.SelectDeviceUseCase
import jp.kaleidot725.adbpad.domain.usecase.device.UpdateDevicesUseCase
import jp.kaleidot725.adbpad.domain.usecase.scrcpy.GetScrcpyTierPresetsUseCase
import jp.kaleidot725.adbpad.domain.usecase.scrcpy.LaunchScrcpyUseCase
import jp.kaleidot725.adbpad.domain.usecase.scrcpy.ProfileDeviceUseCase
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
    private val checkDeviceLivenessUseCase: CheckDeviceLivenessUseCase,
    private val restartDeviceUseCase: RestartDeviceUseCase,
    private val getScrcpyTierPresetsUseCase: GetScrcpyTierPresetsUseCase,
    private val profileDeviceUseCase: ProfileDeviceUseCase,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) : PulseStore<TopState, TopAction, TopSideEffect, AppBroadCast, AppUnicast>(TopState()) {
    private var deviceJob: Job? = null
    private var selectedDeviceJob: Job? = null

    override fun onSetup() {
        collectDevices()
        coroutineScope.launch {
            val presets = getScrcpyTierPresetsUseCase()
            update { copy(scrcpyTierPresets = presets) }
        }
    }

    override fun onAction(uiAction: TopAction) {
        coroutineScope.launch {
            when (uiAction) {
                is TopAction.ExecuteCommand -> executeCommand(uiAction.command)
                is TopAction.SelectDevice -> selectDevice(uiAction.device)
                TopAction.LaunchScrcpy -> launchScrcpy()
                TopAction.OpenScrcpyTierDialog -> update { copy(showScrcpyTierDialog = true) }
                TopAction.CloseScrcpyTierDialog -> update { copy(showScrcpyTierDialog = false) }
                is TopAction.LaunchScrcpyWithTier -> launchScrcpyWithTier(uiAction.level)
                TopAction.ScanDeviceProfile -> scanDeviceProfile()
                TopAction.LaunchScrcpyWithProfile -> launchScrcpyWithProfile()
                TopAction.CheckDeviceLiveness -> checkDeviceLiveness()
                TopAction.RestartDevice -> restartDevice()
                TopAction.Refresh -> unicast(AppUnicast.Refresh)
                TopAction.OpenWirelessAdb -> update { copy(showWirelessAdbDialog = true, wirelessAdbStatus = "") }
                TopAction.CloseWirelessAdb ->
                    update {
                        copy(
                            showWirelessAdbDialog = false,
                            wirelessAdbStatus = "",
                            wirelessAdbLoading = false,
                        )
                    }
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
            if (!launchScrcpyUseCase(device)) println("Failed to launch Scrcpy for ${device.serial}")
        } catch (e: Exception) {
            println("Failed to launch Scrcpy: ${e.message}")
        }
    }

    private suspend fun launchScrcpyWithTier(level: ScrcpyTierLevel) {
        val device = currentState.selectedDevice ?: return
        update { copy(showScrcpyTierDialog = false) }
        try {
            if (!launchScrcpyUseCase(device, currentState.scrcpyTierPresets.get(level))) {
                println("Failed to launch Scrcpy for ${device.serial}")
            }
        } catch (e: Exception) {
            println("Failed to launch Scrcpy: ${e.message}")
        }
    }

    private suspend fun scanDeviceProfile() {
        val device = currentState.selectedDevice ?: return
        update { copy(isProfilingDevice = true, profilingStatus = "Starting scan...") }
        try {
            val profile = profileDeviceUseCase(device) { status -> update { copy(profilingStatus = status) } }
            // ponytail: keep the specific onProgress message (e.g. "no encoder survived connect")
            // instead of clobbering it with a generic string - that's the actual diagnostic.
            update { copy(deviceProfile = profile, isProfilingDevice = false) }
        } catch (e: Exception) {
            println("Failed to profile device ${device.serial}: ${e.message}")
            update { copy(isProfilingDevice = false, profilingStatus = "Scan failed: ${e.message}") }
        }
    }

    private suspend fun launchScrcpyWithProfile() {
        val device = currentState.selectedDevice ?: return
        val profile = currentState.deviceProfile ?: return
        update { copy(showScrcpyTierDialog = false) }
        try {
            if (!launchScrcpyUseCase(device, profile.toTierPreset())) {
                println("Failed to launch Scrcpy for ${device.serial}")
            }
        } catch (e: Exception) {
            println("Failed to launch Scrcpy: ${e.message}")
        }
    }

    private suspend fun connectWirelessAdb(
        host: String,
        port: Int,
    ) {
        update { copy(wirelessAdbLoading = true, wirelessAdbStatus = "") }
        try {
            val result = connectDeviceUseCase(host, port)
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = result) }
        } catch (e: Exception) {
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = "Error: ${e.message}") }
        }
    }

    private suspend fun pairWirelessAdb(
        host: String,
        port: Int,
        code: String,
    ) {
        update { copy(wirelessAdbLoading = true, wirelessAdbStatus = "") }
        try {
            val result = pairDeviceUseCase(host, port, code)
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = result) }
        } catch (e: Exception) {
            update { copy(wirelessAdbLoading = false, wirelessAdbStatus = "Error: ${e.message}") }
        }
    }

    private suspend fun disconnectWirelessAdb(
        host: String,
        port: Int,
    ) {
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
                getSelectedDeviceFlowUseCase().collect { device ->
                    update {
                        copy(
                            selectedDevice = device,
                            deviceLiveness = DeviceLiveness.UNKNOWN,
                            deviceProfile = null,
                            profilingStatus = "",
                        )
                    }
                    if (device != null) {
                        checkDeviceLiveness()
                        val profile = deviceSettingsRepository.getDeviceSettings(device).scrcpyProfile
                        if (currentState.selectedDevice == device) update { copy(deviceProfile = profile) }
                    }
                }
            }
    }

    private suspend fun checkDeviceLiveness() {
        val device = currentState.selectedDevice ?: return
        update { copy(deviceLiveness = DeviceLiveness.CHECKING) }
        val liveness = checkDeviceLivenessUseCase(device)
        if (currentState.selectedDevice == device) {
            update { copy(deviceLiveness = liveness) }
        }
    }

    private suspend fun restartDevice() {
        val device = currentState.selectedDevice ?: return
        restartDeviceUseCase(device)
        update { copy(deviceLiveness = DeviceLiveness.CHECKING) }
        delay(RESTART_RECHECK_DELAY)
        if (currentState.selectedDevice == device) {
            checkDeviceLiveness()
        }
    }

    companion object {
        private const val RESTART_RECHECK_DELAY = 15_000L
    }
}
