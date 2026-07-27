package jp.kaleidot725.adbpad.domain.usecase.scrcpy

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPreset
import jp.kaleidot725.adbpad.domain.repository.AdbBinary
import jp.kaleidot725.adbpad.domain.repository.DeviceSettingsRepository
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepository
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.scrcpykt.ScrcpyClient
import jp.kaleidot725.scrcpykt.ScrcpyResult

class LaunchScrcpyUseCase(
    private val settingRepository: SettingRepository,
    private val scrcpyProcessRepository: ScrcpyProcessRepository,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) {
    suspend operator fun invoke(device: Device, tierPreset: ScrcpyTierPreset? = null): Boolean {
        scrcpyProcessRepository.getProcess(device.serial)?.terminate()

        val scrcpySettings = settingRepository.getScrcpySettings()
        val scrcpyPath = scrcpySettings.binaryPath

        val adbSettings = settingRepository.getSdkPath()
        val adbPath = adbSettings.adbDirectory

        // ponytail: kill any orphaned scrcpy-server left on the device from a previous
        // session that didn't shut down cleanly (e.g. client force-killed) - a stale
        // server still holding the display/encoder can hang a weak Android TV SoC when
        // a second capture session starts on top of it.
        killStaleScrcpyServer(device.serial)

        val deviceSettings = deviceSettingsRepository.getDeviceSettings(device)
        val scrcpyOptions = tierPreset?.applyTo(deviceSettings.scrcpyOptions) ?: deviceSettings.scrcpyOptions

        val displayName = deviceSettings.customName ?: device.name
        val client = ScrcpyClient.create(binaryPath = scrcpyPath, adbPath = adbPath)
        val result =
            client.mirror {
                connection {
                    serial(device.serial)
                }

                video {
                    scrcpyOptions.maxSize?.let { maxSize(it) }
                    scrcpyOptions.videoBitRate?.let { bitRate(it) }
                    scrcpyOptions.maxFps?.let { maxFps(it) }
                    scrcpyOptions.videoCodec?.let { codec(it) }
                    scrcpyOptions.videoSource?.let { source(it) }
                    if (scrcpyOptions.noVideo) disableVideo()
                }

                audio {
                    if (scrcpyOptions.noAudio) {
                        disableAudio()
                    } else {
                        scrcpyOptions.audioBitRate?.let { bitRate(it) }
                        scrcpyOptions.audioCodec?.let { codec(it) }
                        scrcpyOptions.audioSource?.let { source(it) }
                        scrcpyOptions.audioBuffer?.let { buffer(it) }
                    }
                }

                display {
                    val title = scrcpyOptions.windowTitle ?: "$displayName - ${device.serial}"
                    windowTitle(title)
                    scrcpyOptions.displayId?.let { displayId(it) }
                    scrcpyOptions.windowX?.let { x ->
                        scrcpyOptions.windowY?.let { y -> windowPosition(x, y) }
                    }
                    scrcpyOptions.windowWidth?.let { width ->
                        scrcpyOptions.windowHeight?.let { height -> windowSize(width, height) }
                    }
                    if (scrcpyOptions.alwaysOnTop) alwaysOnTop()
                    if (scrcpyOptions.fullscreen) fullscreen()
                }

                control {
                    if (scrcpyOptions.stayAwake) stayAwake()
                    if (scrcpyOptions.turnScreenOff) turnScreenOff()
                    if (scrcpyOptions.powerOffOnClose) powerOffOnClose()
                    if (scrcpyOptions.showTouches) showTouches()
                    if (scrcpyOptions.disableScreensaver) disableScreensaver()
                }
            }

        return when (result) {
            is ScrcpyResult.Success -> {
                scrcpyProcessRepository.storeProcess(device.serial, result.process)
                true
            }

            is ScrcpyResult.Error -> {
                false
            }
        }
    }

    private suspend fun killStaleScrcpyServer(serial: String) {
        try {
            AdbBinary.processBuilder(settingRepository, "-s", serial, "shell", "pkill", "-f", "app_process.*scrcpy")
                .start()
                .waitFor()
        } catch (_: Exception) {
            // best-effort cleanup, ignore if the device doesn't have pkill or nothing to kill
        }
    }
}
