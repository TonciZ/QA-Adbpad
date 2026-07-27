package jp.kaleidot725.adbpad.domain.usecase.scrcpy

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.ScrcpyTierPreset
import jp.kaleidot725.adbpad.domain.repository.AdbBinary
import jp.kaleidot725.adbpad.domain.repository.DeviceSettingsRepository
import jp.kaleidot725.adbpad.domain.repository.ScrcpyProcessRepository
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.scrcpykt.ScrcpyClient
import jp.kaleidot725.scrcpykt.ScrcpyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LaunchScrcpyUseCase(
    private val settingRepository: SettingRepository,
    private val scrcpyProcessRepository: ScrcpyProcessRepository,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) {
    suspend operator fun invoke(
        device: Device,
        tierPreset: ScrcpyTierPreset? = null,
    ): Boolean {
        scrcpyProcessRepository.getProcess(device.serial)?.terminate()

        val scrcpySettings = settingRepository.getScrcpySettings()
        val scrcpyPath = scrcpySettings.binaryPath

        val adbSettings = settingRepository.getSdkPath()
        val adbPath = adbSettings.adbDirectory

        // ponytail: this freeze reproduces with plain native scrcpy too (confirmed - not
        // specific to our app or to profiled devices), so every launch gets the same reset,
        // every time, regardless of whether a profile exists: kill any leftover on-device
        // scrcpy-server AND drop any adb reverse/forward tunnels it left behind. A stale
        // tunnel from a killed session can make the *next* connect's control channel hang
        // while video is already being pushed - the device looks frozen even though adb
        // itself stays responsive.
        flushDeviceConnectionState(device.serial)

        val deviceSettings = deviceSettingsRepository.getDeviceSettings(device)
        val profile = deviceSettings.scrcpyProfile
        val baseOptions =
            if (profile != null) deviceSettings.scrcpyOptions.copy(videoEncoder = profile.videoEncoder) else deviceSettings.scrcpyOptions
        val scrcpyOptions = tierPreset?.applyTo(baseOptions) ?: baseOptions

        // ponytail: root-cause fix from raw scrcpy testing - some Android TV SoCs (Amlogic
        // vendor hw encoder) freeze if scrcpy connects immediately after `adb connect`/app
        // launch. Pace every connection the same way; use the profile's tuned settle delay
        // when one exists, otherwise a safe default.
        pacedConnect(device.serial, profile?.settleDelayMs ?: DEFAULT_SETTLE_DELAY_MS)

        val displayName = deviceSettings.customName ?: device.name
        val client = ScrcpyClient.create(binaryPath = scrcpyPath, adbPath = adbPath)
        // ponytail: mirror() is a blocking call with no internal timeout - if the device-side
        // scrcpy-server never responds (frozen/weak TV box), it hangs forever. Bound it so the
        // app doesn't appear to hang along with the device, and clean up the stale server after.
        val result =
            withTimeoutOrNull(LAUNCH_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
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
                            scrcpyOptions.videoEncoder?.let { encoder(it) }
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
                }
            }

        if (result == null) {
            println("Scrcpy launch timed out for ${device.serial} after ${LAUNCH_TIMEOUT_MS}ms - device likely unresponsive")
            flushDeviceConnectionState(device.serial)
            return false
        }

        return when (result) {
            is ScrcpyResult.Success -> {
                scrcpyProcessRepository.storeProcess(device.serial, result.process)
                true
            }

            is ScrcpyResult.Error -> {
                println("Scrcpy launch failed for ${device.serial}: ${result.message}")
                result.exception.printStackTrace()
                false
            }
        }
    }

    private suspend fun pacedConnect(
        serial: String,
        settleDelayMs: Long,
    ) {
        delay(settleDelayMs)
        try {
            val process = AdbBinary.processBuilder(settingRepository, "-s", serial, "shell", "echo", "warmup").start()
            process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {
            // best-effort readiness ping, proceed to launch regardless
        }
        delay(READINESS_PING_SETTLE_MS)
    }

    private suspend fun flushDeviceConnectionState(serial: String) {
        runAdbBestEffort(serial, "shell", "pkill", "-f", "app_process.*scrcpy")
        // ponytail: scrcpy tunnels its control/video socket via adb reverse (falls back to
        // forward) - a killed session can leave that tunnel bound to a dead local port, which
        // then wedges the next session's control channel while video keeps streaming.
        runAdbBestEffort(serial, "reverse", "--remove-all")
        runAdbBestEffort(serial, "forward", "--remove-all")
    }

    private suspend fun runAdbBestEffort(
        serial: String,
        vararg args: String,
    ) {
        try {
            val process = AdbBinary.processBuilder(settingRepository, "-s", serial, *args).start()
            // ponytail: a wedged device can leave any of these shell/reverse/forward calls
            // hanging forever with no timeout, blocking every launch attempt. Bound them.
            if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        } catch (_: Exception) {
            // best-effort cleanup, ignore if the device/adb doesn't support it or there's nothing to clean
        }
    }

    companion object {
        private const val LAUNCH_TIMEOUT_MS = 15_000L
        private const val READINESS_PING_SETTLE_MS = 2_000L
        private const val DEFAULT_SETTLE_DELAY_MS = 3_000L
    }
}
