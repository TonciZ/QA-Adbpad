package jp.kaleidot725.adbpad.ui.screen.log.state

import jp.kaleidot725.pulse.mvi.PulseAction

sealed class LogAction : PulseAction {
    data object StartCapture : LogAction()
    data object StopCapture : LogAction()
    data object ClearLog : LogAction()
    data class UpdateFilter(val filter: String) : LogAction()
}
