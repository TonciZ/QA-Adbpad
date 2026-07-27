package jp.kaleidot725.adbpad.domain.usecase.scrcpy

import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.model.device.DeviceProfile
import jp.kaleidot725.adbpad.domain.repository.AdbBinary
import jp.kaleidot725.adbpad.domain.repository.DeviceSettingsRepository
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.scrcpykt.ScrcpyClient
import jp.kaleidot725.scrcpykt.ScrcpyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

// ponytail: root-caused on a real Amlogic Android TV box (see .planning/SCRCPY_AUTO_PROFILER_HANDOVER.md) -
// vendor hw h264 encoders can freeze the device on connect unless the connection is paced, and some
// SoCs cap out at a lower bitrate than the fixed tiers assume. This scans a misbehaving device once and
// derives a working custom profile (encoder + bitrate ceiling + settle delay) instead of asking the user
// to hand-tune scrcpy flags.
class ProfileDeviceUseCase(
    private val settingRepository: SettingRepository,
    private val deviceSettingsRepository: DeviceSettingsRepository,
) {
    suspend operator fun invoke(
        device: Device,
        onProgress: (String) -> Unit = {},
    ): DeviceProfile? =
        withContext(Dispatchers.IO) {
            val scrcpyPath = settingRepository.getScrcpySettings().binaryPath
            onProgress("Listing device encoders...")
            val encoders = listH264Encoders(scrcpyPath, device.serial)
            if (encoders.isEmpty()) {
                onProgress("No h264 encoders reported by device")
                return@withContext null
            }

            var winner: String? = null
            for (encoder in encoders) {
                onProgress("Testing $encoder @ ${INITIAL_BIT_RATE / 1_000_000}Mbps...")
                if (probe(scrcpyPath, device.serial, encoder, INITIAL_BIT_RATE, PROBE_FPS, SETTLE_DELAY_MS)) {
                    winner = encoder
                    break
                }
                onProgress("$encoder failed, trying next candidate...")
            }
            if (winner == null) {
                onProgress("No encoder survived connect - device may need manual tuning")
                return@withContext null
            }
            val encoder = winner

            val bitRateSteps = listOf(INITIAL_BIT_RATE) + BIT_RATE_RAMP
            var highestPassingIndex = 0
            for (index in 1 until bitRateSteps.size) {
                val bitRate = bitRateSteps[index]
                onProgress("$encoder OK, pushing bitrate to ${bitRate / 1_000_000}Mbps...")
                if (probe(scrcpyPath, device.serial, encoder, bitRate, PROBE_FPS, SETTLE_DELAY_MS)) {
                    highestPassingIndex = index
                } else {
                    onProgress("${bitRate / 1_000_000}Mbps failed, stopping ramp")
                    break
                }
            }
            // ponytail: back off one notch from the highest surviving step for margin -
            // "just barely worked once" isn't the same as "reliably works".
            val safeBitRate = bitRateSteps[if (highestPassingIndex > 0) highestPassingIndex - 1 else 0]

            // ponytail: the bug this profiler exists for showed up specifically on a *second*
            // connect right after closing the first (see .planning handover) - a single successful
            // probe doesn't prove that. Do one more automated close-then-reopen cycle at the
            // chosen settings before trusting the profile.
            onProgress("Confirming reconnect (close & reopen) at $encoder, ${safeBitRate / 1_000_000}Mbps...")
            if (!probe(scrcpyPath, device.serial, encoder, safeBitRate, PROBE_FPS, SETTLE_DELAY_MS)) {
                onProgress("Reconnect failed at $encoder, ${safeBitRate / 1_000_000}Mbps - device may need manual tuning")
                return@withContext null
            }

            val profile =
                DeviceProfile(
                    videoEncoder = encoder,
                    safeBitRate = safeBitRate,
                    safeMaxFps = PROBE_FPS,
                    settleDelayMs = SETTLE_DELAY_MS,
                )
            val settings = deviceSettingsRepository.getDeviceSettings(device)
            deviceSettingsRepository.saveDeviceSettings(device, settings.copy(scrcpyProfile = profile))
            onProgress("Profile saved: $encoder, ${safeBitRate / 1_000_000}Mbps, ${PROBE_FPS}fps")
            profile
        }

    private fun listH264Encoders(
        scrcpyPath: String,
        serial: String,
    ): List<String> {
        val output =
            try {
                val process = ProcessBuilder(scrcpyPath, "-s", serial, "--list-encoders").start()
                if (!process.waitFor(LIST_ENCODERS_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return emptyList()
                }
                process.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                return emptyList()
            }

        val matches =
            output
                .lineSequence()
                .filterNot { it.contains("alias for") }
                .mapNotNull { ENCODER_LINE_REGEX.find(it) }
                .toList()
        val hw = matches.filter { it.groupValues[2] == "hw" }.map { it.groupValues[1] }
        val sw = matches.filter { it.groupValues[2] == "sw" }.map { it.groupValues[1] }
        return hw + sw
    }

    private suspend fun probe(
        scrcpyPath: String,
        serial: String,
        encoder: String,
        bitRate: Int,
        fps: Int,
        settleDelayMs: Long,
    ): Boolean {
        killStaleScrcpyServer(serial)
        pacedConnect(serial, settleDelayMs)

        val client = ScrcpyClient.create(binaryPath = scrcpyPath, adbPath = settingRepository.getSdkPath().adbDirectory)
        val result =
            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    client.mirror {
                        connection { serial(serial) }
                        video {
                            bitRate(bitRate)
                            maxFps(fps)
                            encoder(encoder)
                        }
                        audio { disableAudio() }
                        control { stayAwake() }
                    }
                }
            }

        if (result !is ScrcpyResult.Success) {
            killStaleScrcpyServer(serial)
            return false
        }
        result.process.terminate()

        return isResponsive(serial)
    }

    private suspend fun isResponsive(serial: String): Boolean =
        try {
            val process = AdbBinary.processBuilder(settingRepository, "-s", serial, "shell", "echo", "ping").start()
            process.waitFor(RESPONSIVENESS_TIMEOUT_SEC, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (_: Exception) {
            false
        }

    private suspend fun pacedConnect(
        serial: String,
        settleDelayMs: Long,
    ) {
        delay(settleDelayMs)
        try {
            val process = AdbBinary.processBuilder(settingRepository, "-s", serial, "shell", "echo", "warmup").start()
            process.waitFor(3, TimeUnit.SECONDS)
        } catch (_: Exception) {
            // best-effort readiness ping, proceed regardless
        }
        delay(READINESS_SETTLE_MS)
    }

    private suspend fun killStaleScrcpyServer(serial: String) {
        runAdbBestEffort(serial, "shell", "pkill", "-f", "app_process.*scrcpy")
        // ponytail: a killed scrcpy session can leave its adb reverse/forward tunnel bound to
        // a dead local port, wedging the next session's control channel while video keeps
        // streaming - confirmed to freeze plain native scrcpy too, not just through this app.
        runAdbBestEffort(serial, "reverse", "--remove-all")
        runAdbBestEffort(serial, "forward", "--remove-all")
    }

    private suspend fun runAdbBestEffort(
        serial: String,
        vararg args: String,
    ) {
        try {
            val process = AdbBinary.processBuilder(settingRepository, "-s", serial, *args).start()
            if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly()
        } catch (_: Exception) {
            // best-effort cleanup
        }
    }

    companion object {
        // ponytail: real `scrcpy --list-encoders` output has no quotes around the encoder name
        // and trailing tags like "[vendor]" or "(alias for ...)" - confirmed against a real
        // Amlogic TV box, format assumed from docs alone was wrong (always matched zero lines).
        private val ENCODER_LINE_REGEX = Regex("""--video-codec=h264 --video-encoder=(\S+)\s+\((hw|sw)\)""")
        private const val INITIAL_BIT_RATE = 2_000_000
        private val BIT_RATE_RAMP = listOf(4_000_000, 6_000_000, 8_000_000, 12_000_000)
        private const val PROBE_FPS = 25
        private const val SETTLE_DELAY_MS = 8_000L
        private const val READINESS_SETTLE_MS = 2_000L
        private const val PROBE_TIMEOUT_MS = 15_000L
        private const val LIST_ENCODERS_TIMEOUT_SEC = 10L
        private const val RESPONSIVENESS_TIMEOUT_SEC = 5L
    }
}
