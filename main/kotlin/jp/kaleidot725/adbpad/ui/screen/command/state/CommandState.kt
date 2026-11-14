package jp.kaleidot725.adbpad.ui.screen.command.state

import jp.kaleidot725.adbpad.domain.model.command.CommandExecutionHistory
import jp.kaleidot725.adbpad.domain.model.command.NormalCommandCategory
import jp.kaleidot725.adbpad.domain.model.command.NormalCommandGroup
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.ui.screen.command.model.CommandLayoutMode
import jp.kaleidot725.pulse.mvi.PulseState

data class CommandState(
    val commands: NormalCommandGroup = NormalCommandGroup.Empty,
    val filtered: NormalCommandCategory = NormalCommandCategory.ALL,
    val selectedDevice: Device? = null,
    val layoutMode: CommandLayoutMode = CommandLayoutMode.LIST,
    val executionHistory: CommandExecutionHistory? = null,
    val toggleStates: Map<String, Boolean?> = emptyMap(),
    val isCustomCommandRunning: Boolean = false,
    // Newest first.
    val customCommandHistory: List<String> = emptyList(),
) : PulseState {
    val canExecuteCommand: Boolean get() = selectedDevice != null
}
