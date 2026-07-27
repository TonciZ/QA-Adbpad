package jp.kaleidot725.adbpad.di

import jp.kaleidot725.adbpad.ui.screen.app.AppStateHolder
import jp.kaleidot725.adbpad.ui.screen.command.CommandStateHolder
import jp.kaleidot725.adbpad.ui.screen.device.DeviceSettingsStateHolder
import jp.kaleidot725.adbpad.ui.screen.log.LogStateHolder
import jp.kaleidot725.adbpad.ui.screen.main.MainStateHolder
import jp.kaleidot725.adbpad.ui.screen.newdisplay.ScrcpyNewDisplayStateHolder
import jp.kaleidot725.adbpad.ui.screen.screenshot.ScreenshotStateHolder
import jp.kaleidot725.adbpad.ui.screen.setting.SettingStateHolder
import jp.kaleidot725.adbpad.ui.screen.text.TextCommandStateHolder
import jp.kaleidot725.adbpad.ui.section.top.TopStateHolder
import org.koin.dsl.module

val stateHolderModule =
    module {
        factory {
            CommandStateHolder(
                getNormalCommandGroup = get(),
                toggleNormalCommandFavorite = get(),
                executeCommandUseCase = get(),
                getSelectedDeviceFlowUseCase = get(),
                normalCommandOutputRepository = get(),
                normalCommandRepository = get(),
            )
        }

        factory {
            AppStateHolder(
                getSelectedDeviceFlowUseCase = get(),
                installedAppRepository = get(),
            )
        }

        factory {
            TextCommandStateHolder(
                textCommandRepository = get(),
                getTextCommandUseCase = get(),
                executeTextCommandUseCase = get(),
                getSelectedDeviceFlowUseCase = get(),
            )
        }

        factory {
            ScreenshotStateHolder(
                takeScreenshotUseCase = get(),
                getSelectedDeviceFlowUseCase = get(),
                screenshotCommandRepository = get(),
                renameScreenshotUseCase = get(),
            )
        }

        factory {
            ScrcpyNewDisplayStateHolder(
                getSelectedDeviceFlowUseCase = get(),
                getScrcpyNewDisplayProfilesUseCase = get(),
                launchScrcpyNewDisplayUseCase = get(),
                saveScrcpyNewDisplayProfileUseCase = get(),
                deleteScrcpyNewDisplayProfileUseCase = get(),
            )
        }

        factory {
            SettingStateHolder(
                getSdkPathUseCase = get(),
                saveSdkPathUseCase = get(),
                getAppearanceUseCase = get(),
                saveAppearanceUseCase = get(),
                getLanguageUseCase = get(),
                saveLanguageUseCase = get(),
                getScrcpySettingsUseCase = get(),
                saveScrcpySettingsUseCase = get(),
                restartAdbUseCase = get(),
                getAccentColorUseCase = get(),
                saveAccentColorUseCase = get(),
                getScrcpyTierPresetsUseCase = get(),
                saveScrcpyTierPresetsUseCase = get(),
            )
        }

        factory {
            TopStateHolder(
                updateDevicesUseCase = get(),
                getSelectedDeviceFlowUseCase = get(),
                selectDeviceUseCase = get(),
                executeDeviceControlCommandUseCase = get(),
                launchScrcpyUseCase = get(),
                connectDeviceUseCase = get(),
                pairDeviceUseCase = get(),
                disconnectDeviceUseCase = get(),
                checkDeviceLivenessUseCase = get(),
                restartDeviceUseCase = get(),
                getScrcpyTierPresetsUseCase = get(),
                profileDeviceUseCase = get(),
                deviceSettingsRepository = get(),
            )
        }

        factory {
            LogStateHolder(
                getSelectedDeviceFlowUseCase = get(),
                logCaptureRepository = get(),
            )
        }

        factory {
            DeviceSettingsStateHolder(
                getSelectedDeviceFlowUseCase = get(),
                deviceSettingsRepository = get(),
            )
        }

        factory {
            MainStateHolder(
                getWindowSizeUseCase = get(),
                saveWindowSizeUseCase = get(),
                startAdbUseCase = get(),
                getDarkModeFlowUseCase = get(),
                getLanguageUseCase = get(),
                getAccentColorUseCase = get(),
                refreshUseCase = get(),
                shutdownAppUseCase = get(),
            )
        }
    }
