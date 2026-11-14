package jp.kaleidot725.adbpad.di

import jp.kaleidot725.adbpad.data.repository.DeviceSettingsRepositoryImpl
import jp.kaleidot725.adbpad.data.repository.InstalledAppRepositoryImpl
import jp.kaleidot725.adbpad.data.repository.NormalCommandFavoriteRepositoryImpl
import jp.kaleidot725.adbpad.data.repository.SettingRepositoryImpl
import jp.kaleidot725.adbpad.data.repository.TextCommandRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.AdbClientProvider
import jp.kaleidot725.adbpad.domain.repository.CustomCommandRepository
import jp.kaleidot725.adbpad.domain.repository.CustomCommandRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.DeviceConnectionRepository
import jp.kaleidot725.adbpad.domain.repository.DeviceConnectionRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.DeviceControlCommandRepository
import jp.kaleidot725.adbpad.domain.repository.DeviceControlCommandRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.DeviceHealthRepository
import jp.kaleidot725.adbpad.domain.repository.DeviceHealthRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.DeviceRepository
import jp.kaleidot725.adbpad.domain.repository.DeviceRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.DeviceSettingsRepository
import jp.kaleidot725.adbpad.domain.repository.InstalledAppRepository
import jp.kaleidot725.adbpad.domain.repository.LogCaptureRepository
import jp.kaleidot725.adbpad.domain.repository.LogCaptureRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.NormalCommandFavoriteRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandOutputRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandOutputRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.NormalCommandRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepository
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.ScreenshotCommandRepository
import jp.kaleidot725.adbpad.domain.repository.ScreenshotCommandRepositoryImpl
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.adbpad.domain.repository.TextCommandRepository
import org.koin.dsl.module

val repositoryModule =
    module {
        single { AdbClientProvider(settingRepository = get()) }
        single<DeviceRepository> {
            DeviceRepositoryImpl(deviceSettingsRepository = get(), adbClientProvider = get())
        }
        single<NormalCommandOutputRepository> {
            NormalCommandOutputRepositoryImpl()
        }
        single<NormalCommandRepository> {
            NormalCommandRepositoryImpl(adbClientProvider = get())
        }
        single<CustomCommandRepository> {
            CustomCommandRepositoryImpl(settingRepository = get())
        }
        single<InstalledAppRepository> {
            InstalledAppRepositoryImpl()
        }
        single<NormalCommandFavoriteRepository> {
            NormalCommandFavoriteRepositoryImpl()
        }
        single<TextCommandRepository> {
            TextCommandRepositoryImpl(get())
        }
        single<ScreenshotCommandRepository> {
            ScreenshotCommandRepositoryImpl(settingRepository = get())
        }
        factory<SettingRepository> {
            SettingRepositoryImpl()
        }
        factory<DeviceControlCommandRepository> {
            DeviceControlCommandRepositoryImpl(adbClientProvider = get())
        }
        single<DeviceConnectionRepository> {
            DeviceConnectionRepositoryImpl(adbClientProvider = get())
        }
        single<LogCaptureRepository> {
            LogCaptureRepositoryImpl(get())
        }
        single<ScrcpyProcessRepository> {
            ScrcpyProcessRepositoryImpl()
        }
        factory<DeviceSettingsRepository> {
            DeviceSettingsRepositoryImpl()
        }
        single<DeviceHealthRepository> {
            DeviceHealthRepositoryImpl(settingRepository = get())
        }
    }
