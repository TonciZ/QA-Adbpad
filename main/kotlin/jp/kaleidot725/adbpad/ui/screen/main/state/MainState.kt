package jp.kaleidot725.adbpad.ui.screen.main.state

import jp.kaleidot725.adbpad.domain.model.MainCategory
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.language.Language
import jp.kaleidot725.adbpad.domain.model.setting.AccentColor
import jp.kaleidot725.adbpad.domain.model.setting.WindowSize
import jp.kaleidot725.adbpad.domain.model.update.UpdateInfo
import jp.kaleidot725.pulse.mvi.PulseState

data class MainState(
    val language: Language.Type = Language.Type.ENGLISH,
    val isDark: Boolean? = null,
    val size: WindowSize = WindowSize.UNKNOWN,
    val dialog: MainDialog = MainDialog.Empty,
    val category: MainCategory = MainCategory.Command,
    val selectedDevice: Device? = null,
    val isAlwaysOnTop: Boolean = false,
    // Open by default: icons alone do not tell a first-time user what each section is.
    val isNavigationRailCollapsed: Boolean = false,
    val accentColor: AccentColor = AccentColor.BLUE,
    // Found by the check at launch; null when up to date, offline, or dismissed for this run.
    val availableUpdate: UpdateInfo? = null,
    val isInstallingUpdate: Boolean = false,
    val updateFailed: Boolean = false,
    // The previous run started the Windows installer and it did not finish (e.g. permission refused).
    val lastInstallFailed: Boolean = false,
) : PulseState
