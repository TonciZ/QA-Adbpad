package jp.kaleidot725.adbpad.ui.screen.log.state

import jp.kaleidot725.adbpad.domain.model.log.LogLevel
import jp.kaleidot725.pulse.mvi.PulseAction

sealed class LogAction : PulseAction {
    data object StartCapture : LogAction()

    data object StopCapture : LogAction()

    data object ClearLog : LogAction()

    data object OpenSaveDialog : LogAction()

    data object CloseSaveDialog : LogAction()

    data class UpdateSaveFileName(
        val fileName: String,
    ) : LogAction()

    data class SetSaveOnlyShown(
        val onlyShown: Boolean,
    ) : LogAction()

    data object ChooseSaveDirectory : LogAction()

    data object ConfirmSave : LogAction()

    data object OpenSavedFolder : LogAction()

    data class UpdateFilter(
        val filter: String,
    ) : LogAction()

    data class UpdateSearch(
        val search: String,
    ) : LogAction()

    data class UpdateMinLevel(
        val level: LogLevel,
    ) : LogAction()

    data class SetAutoScroll(
        val enabled: Boolean,
    ) : LogAction()

    data class SetWrapLines(
        val enabled: Boolean,
    ) : LogAction()
}
