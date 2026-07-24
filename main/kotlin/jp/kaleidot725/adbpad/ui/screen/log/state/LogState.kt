package jp.kaleidot725.adbpad.ui.screen.log.state

import jp.kaleidot725.pulse.mvi.PulseState

data class LogState(
    val lines: List<String> = emptyList(),
    val isCapturing: Boolean = false,
    val filter: String = "",
    val savedFile: String = "",
) : PulseState
