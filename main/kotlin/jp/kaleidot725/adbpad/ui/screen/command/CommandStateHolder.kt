package jp.kaleidot725.adbpad.ui.screen.command

import jp.kaleidot725.adbpad.domain.model.command.NormalCommand
import jp.kaleidot725.adbpad.domain.model.command.NormalCommandCategory
import jp.kaleidot725.adbpad.domain.model.command.ToggleCommandDef
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.repository.NormalCommandOutputRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandRepository
import jp.kaleidot725.adbpad.domain.usecase.command.ExecuteCommandUseCase
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
import kotlinx.coroutines.launch

class CommandStateHolder(
    private val getNormalCommandGroup: GetNormalCommandGroup,
    private val toggleNormalCommandFavorite: ToggleNormalCommandFavorite,
    private val executeCommandUseCase: ExecuteCommandUseCase,
    private val getSelectedDeviceFlowUseCase: GetSelectedDeviceFlowUseCase,
    private val normalCommandOutputRepository: NormalCommandOutputRepository,
    private val normalCommandRepository: NormalCommandRepository,
) : PulseStore<CommandState, CommandAction, CommandSideEffect, AppBroadCast, AppUnicast>(initialUiState = CommandState()) {
    override fun onSetup() {
        coroutineScope.launch {
            getSelectedDeviceFlowUseCase().collect { device ->
                update { this.copy(selectedDevice = device) }
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

        update { copy(toggleStates = toggleStates + (def.onKey to !isCurrentlyOn)) }

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
                refreshToggleState(selectedDevice, def)
            },
            onComplete = {
                val commands = getNormalCommandGroup()
                update { copy(commands = commands) }
                refreshToggleState(selectedDevice, def)
            },
        )
    }

    // ponytail: sequential queries; parallel if >2s on real device
    private suspend fun queryToggleStates(device: Device) {
        val states = mutableMapOf<String, Boolean?>()
        for (def in ToggleCommandDef.ALL) {
            try {
                val output = normalCommandRepository.queryShell(device, def.queryCommand)
                states[def.onKey] = def.parseIsOn(output)
            } catch (_: Exception) {
                states[def.onKey] = null
            }
        }
        update { copy(toggleStates = states) }
    }

    private suspend fun refreshToggleState(
        device: Device,
        def: ToggleCommandDef,
    ) {
        try {
            val output = normalCommandRepository.queryShell(device, def.queryCommand)
            val isOn = def.parseIsOn(output)
            update { copy(toggleStates = toggleStates + (def.onKey to isOn)) }
        } catch (_: Exception) {
            update { copy(toggleStates = toggleStates + (def.onKey to null)) }
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
