package jp.kaleidot725.adbpad.ui.screen.command

import jp.kaleidot725.adbpad.domain.model.command.NormalCommand
import jp.kaleidot725.adbpad.domain.model.command.NormalCommandCategory
import jp.kaleidot725.adbpad.domain.model.command.ToggleCommandDef
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.repository.NormalCommandOutputRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandRepository
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteCommandUseCase
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteCustomCommandUseCase
import jp.kaleidot725.adbpad.domain.usecase.command.GetNormalCommandGroup
import jp.kaleidot725.adbpad.domain.usecase.command.ToggleNormalCommandFavorite
import jp.kaleidot725.adbpad.domain.usecase.device.GetSelectedDeviceFlowUseCase
import jp.kaleidot725.adbpad.ui.container.AppBroadCast
import jp.kaleidot725.adbpad.ui.container.AppUnicast
import jp.kaleidot725.adbpad.ui.screen.command.model.CommandLayoutMode
import jp.kaleidot725.adbpad.ui.screen.command.state.CommandAction
import jp.kaleidot725.adbpad.ui.screen.command.state.CommandSideEffect
import jp.kaleidot725.adbpad.ui.screen.command.state.CommandState
import jp.kaleidot725.pulse.mvi.PulseStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CommandStateHolder(
    private val getNormalCommandGroup: GetNormalCommandGroup,
    private val toggleNormalCommandFavorite: ToggleNormalCommandFavorite,
    private val executeCommandUseCase: ExecuteCommandUseCase,
    private val executeCustomCommandUseCase: ExecuteCustomCommandUseCase,
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val normalCommandOutputRepository: NormalCommandOutputRepository,
    private val normalCommandRepository: NormalCommandRepository,
    private val settingRepository: SettingRepository,
) : PulseStore<CommandState, CommandAction, CommandSideEffect, AppBroadCast, AppUnicast>(initialUiState = CommandState()) {
    private var customCommandJob: Job? = null

    // Counts the flips of each switch. A confirmation only reports back while it is still the
    // latest flip of its switch, so a quick second flip cannot be overruled by the first one.
    private val toggleFlips = mutableMapOf<String, Int>()

    private companion object {
        const val MAX_HISTORY = 30
        const val CONFIRM_ATTEMPTS = 4
        const val CONFIRM_INTERVAL_MS = 700L
    }

    override fun onSetup() {
        coroutineScope.launch {
            getSelectedDeviceFlowUseCase().collect { device ->
                update { this.copy(selectedDevice = device, notice = null) }
                if (device != null) {
                    queryToggleStates(device)
                } else {
                    update { copy(toggleStates = emptyMap()) }
                }
            }
        }
        coroutineScope.launch {
            val commands = getNormalCommandGroup()
            update { this.copy(commands = commands) }
        }
        coroutineScope.launch {
            val history = settingRepository.getCustomCommandHistory()
            update { copy(customCommandHistory = history) }
        }
        coroutineScope.launch {
            normalCommandOutputRepository.executionHistory.collect { history ->
                update { this.copy(executionHistory = history) }
            }
        }
    }

    override fun onAction(uiAction: CommandAction) {
        coroutineScope.launch {
            when (uiAction) {
                is CommandAction.ClickCategoryTab -> clickTab(uiAction.category)
                is CommandAction.ExecuteCommand -> executeCommand(uiAction.command)
                is CommandAction.ToggleFavorite -> toggleFavorite(uiAction.command)
                is CommandAction.ToggleSwitch -> executeToggle(uiAction.command)
                is CommandAction.ToggleLayoutMode -> toggleLayoutMode()
                is CommandAction.RunCustomCommand -> runCustomCommand(uiAction.input)
                is CommandAction.StopCustomCommand -> customCommandJob?.cancel()
            }
        }
    }

    override fun onReceive(broadcast: AppBroadCast) {
        when (broadcast) {
            AppBroadCast.Refresh -> {
                coroutineScope.launch {
                    val commands = getNormalCommandGroup()
                    update { this.copy(commands = commands) }
                }
            }
        }
    }

    private suspend fun toggleFavorite(command: NormalCommand) {
        toggleNormalCommandFavorite(command)
        val commands = getNormalCommandGroup()
        update { this.copy(commands = commands) }
    }

    private suspend fun executeCommand(command: NormalCommand) {
        val selectedDevice = state.value.selectedDevice ?: return
        update { copy(notice = null) }
        executeCommandUseCase(
            device = selectedDevice,
            command = command,
            onStart = {
                val commands = getNormalCommandGroup()
                update {
                    this.copy(commands = commands)
                }
            },
            onFailed = {
                val commands = getNormalCommandGroup()
                update {
                    this.copy(commands = commands)
                }
            },
            onComplete = {
                val commands = getNormalCommandGroup()
                update {
                    this.copy(commands = commands)
                }
            },
        )
    }

    private fun clickTab(filtered: NormalCommandCategory) {
        update {
            this.copy(filtered = filtered)
        }
    }

    private suspend fun executeToggle(onCommand: NormalCommand) {
        val selectedDevice = state.value.selectedDevice ?: return
        val def = ToggleCommandDef.ON_KEY_MAP[onCommand.favoriteKey] ?: return
        val isCurrentlyOn = state.value.toggleStates[def.onKey] ?: false
        val commandToExecute = if (isCurrentlyOn) def.createOffCommand() else def.createOnCommand()

        val wanted = !isCurrentlyOn
        val flip = (toggleFlips[def.onKey] ?: 0) + 1
        toggleFlips[def.onKey] = flip
        update { copy(toggleStates = toggleStates + (def.onKey to wanted), notice = null) }

        executeCommandUseCase(
            device = selectedDevice,
            command = commandToExecute,
            onStart = {
                val commands = getNormalCommandGroup()
                update { copy(commands = commands) }
            },
            onFailed = {
                val commands = getNormalCommandGroup()
                update { copy(commands = commands) }
                confirmToggle(selectedDevice, def, wanted, previous = isCurrentlyOn, flip = flip)
            },
            onComplete = {
                val commands = getNormalCommandGroup()
                update { copy(commands = commands) }
                confirmToggle(selectedDevice, def, wanted, previous = isCurrentlyOn, flip = flip)
            },
        )
    }

    /**
     * A command can "succeed" and still change nothing - the setting does not exist on this
     * kind of device (cellular data on a TV stick) or the system refuses it. So the device is
     * asked what the setting is now. Some settings take a moment to switch (Wi-Fi, Bluetooth),
     * hence a few tries. If it never reaches the wanted state, the switch goes back to what
     * the device reports and a plain message says so.
     */
    private suspend fun confirmToggle(
        device: Device,
        def: ToggleCommandDef,
        wanted: Boolean,
        previous: Boolean,
        flip: Int,
    ) {
        var actual: Boolean? = null
        for (attempt in 1..CONFIRM_ATTEMPTS) {
            actual =
                try {
                    def.parseIsOn(normalCommandRepository.queryShell(device, def.queryCommand))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            if (actual == wanted) break
            if (attempt < CONFIRM_ATTEMPTS) delay(CONFIRM_INTERVAL_MS)
        }
        // The user may have switched device or flipped the same switch again while we waited.
        if (state.value.selectedDevice?.serial != device.serial) return
        if (toggleFlips[def.onKey] != flip) return
        if (actual == wanted) {
            update { copy(toggleStates = toggleStates + (def.onKey to wanted)) }
        } else {
            update { copy(toggleStates = toggleStates + (def.onKey to (actual ?: previous)), notice = Language.toggleNotApplied) }
        }
    }

    // ponytail: sequential queries; parallel if >2s on real device
    private suspend fun queryToggleStates(device: Device) {
        val states = mutableMapOf<String, Boolean?>()
        val flipsAtStart = toggleFlips.toMap()
        for (def in ToggleCommandDef.ALL) {
            try {
                val output = normalCommandRepository.queryShell(device, def.queryCommand)
                states[def.onKey] = def.parseIsOn(output)
            } catch (_: Exception) {
                states[def.onKey] = null
            }
        }
        // Switches flipped while this (slow, one query at a time) read was running keep the value
        // their own confirmation set; the read's snapshot of them is older.
        val flippedMeanwhile = toggleFlips.filter { (key, count) -> count != flipsAtStart[key] }.keys
        update { copy(toggleStates = states + toggleStates.filterKeys { it in flippedMeanwhile }) }
    }

    // Deliberately not gated on a selected device: `adb connect` is how a device gets there.
    private fun runCustomCommand(input: String) {
        if (input.isBlank() || customCommandJob?.isActive == true) return
        customCommandJob =
            coroutineScope.launch {
                val entry = input.trim()
                val history = (listOf(entry) + state.value.customCommandHistory.filter { it != entry }).take(MAX_HISTORY)
                update { copy(isCustomCommandRunning = true, customCommandHistory = history, notice = null) }
                try {
                    settingRepository.updateCustomCommandHistory(history)
                    executeCustomCommandUseCase(input) { state.value.selectedDevice?.serial }
                } finally {
                    update { copy(isCustomCommandRunning = false) }
                }
            }
    }

    private fun toggleLayoutMode() {
        update {
            val newMode =
                when (layoutMode) {
                    CommandLayoutMode.CARD -> CommandLayoutMode.LIST
                    CommandLayoutMode.LIST -> CommandLayoutMode.CARD
                }
            this.copy(layoutMode = newMode)
        }
    }
}
