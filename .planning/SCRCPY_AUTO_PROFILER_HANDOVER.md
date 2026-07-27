# Handover: scrcpy Auto-Profiler Feature

Context: raw scrcpy freeze investigation done outside this codebase (separate session, `Projects/ADB/General`), against a Skyworth/Amlogic Android TV box (`192.168.1.227:5555`, Android 12). Goal: turn manual findings into an auto-profiler feature for adbpad's 3-tier (Low/Mid/High) scrcpy launcher, so users whose device fails all 3 presets can run a scan and get a working custom profile.

Raw test log: `C:\Users\zizic\scrcpy-scripts\raw-scrcpy-testing\results.md` (full data, all combos tried).

## Root cause found

Device: Amlogic SoC, encoders available:

- `OMX.amlogic.video.encoder.avc` (h264, hw, vendor)
- `c2.android.avc.encoder` (h264, sw)
- `OMX.amlogic.video.encoder.hevc` (h265, hw, vendor)

**The vendor hw h264 encoder froze the TV on connect whenever the physical screen was on** - reproduced 3/3 times across different tunings (default, low bitrate+res, baseline H264 profile/no B-frames). Not a bitrate/resolution/profile issue on its own.

**Fix: pace the connection.** Insert a settle delay before launching scrcpy:

1. `adb connect`
2. sleep ~8s (let device settle)
3. `adb shell echo warmup` (readiness ping)
4. sleep ~2s
5. _then_ launch scrcpy

With this pacing, the same hw encoder connected clean 4/4 times at low bitrate (2M/24fps). But pushing bitrate too high still froze it even with pacing (12M/30fps froze) - so there's **also** a bitrate/fps ceiling on top of the pacing fix, not isolated which variable (bitrate vs fps) is the actual limit, but bitrate is the more likely culprit (6x jump vs. trivial fps jump). Confirmed working, good-quality config: **6M bitrate / 25fps**, hw encoder, paced connect, screen stays on.

## What worked

- Paced connect (settle delay + readiness ping before launching scrcpy) - eliminates the freeze entirely, confirmed across many repeated runs.
- hw h264 encoder (`OMX.amlogic.video.encoder.avc`) at a moderate bitrate ceiling (6M) + moderate fps (25) - good quality, low input latency, no freeze.
- sw h264 encoder (`c2.android.avc.encoder`) as a fallback - never froze in any test, but hard-capped by the device's own MediaCodec capability query to ~398x224 regardless of `--max-size`/`--video-bit-rate` requested. Confirmed via `wm size` (device is really 1920x1080) - this is a device-side codec capability limit, not something scrcpy flags can override. Usable for basic navigation, unusable for QA screenshot work.
- `-S` (turn-screen-off at connect) also reliably avoided the freeze (this was the _original_ script's accidental workaround) but leaves the physical TV panel dark, which defeats the purpose when the user needs to watch DRM-protected content directly on the TV (mirror capture blacks out DRM/secure surfaces regardless of screen state, so the physical panel is the only way to see that content).

## What didn't work

- Lowering bitrate/resolution alone on the hw encoder (no pacing) - still froze. Confirms it's not purely a load/capacity issue.
- Forcing Baseline H264 profile (`profile=1,level=512`, no B-frames) on the hw encoder (no pacing) - still froze. Rules out profile/B-frame complexity as the cause.
- h265 hw encoder - not tested (skipped after 3 consecutive hw freezes to avoid burning more TV reboots; same vendor encoder family, expected same failure mode).
- `adb shell input keyevent 224` (KEYCODE_WAKEUP) to bring the physical screen back on after `-S` - did nothing. `dumpsys power` showed `mWakefulness=Awake` the whole time; `-S`'s screen-off is implemented through scrcpy's own display-power-mode control channel, not standard Android wakefulness, so standard keyevents can't reverse it. The actual reversal is scrcpy's built-in **right-click on the mirror window = "Power on"** (or `MOD+o` to toggle off while keeping mirroring) - a client-side control message, not an adb command.
- No existing tool/library found online that auto-benchmarks scrcpy encoder settings per device. Searched; only found scattered GitHub issues confirming Amlogic hw encoder problems are a known pattern (e.g. Genymobile/scrcpy#1415), no ready-made profiler.

## adbpad codebase state (as of this handover)

Relevant files:

- `core/domain/.../model/device/ScrcpyTierPreset.kt` - the 3 fixed tiers (Low: 720p/2M/24fps, Mid: 1080p/6M/30fps, High: uncapped/12M/60fps). Note Mid tier's 6M bitrate matches our confirmed-safe number, but at 30fps not 25 - untested whether that 5fps difference matters, and Mid doesn't force the specific encoder or add pacing, so it likely still hits the freeze on affected devices.
- `core/domain/.../model/device/ScrcpyOptions.kt` - per-device persisted options. **No `videoEncoder` field exists yet** - only `videoCodec` (h264/h265/av1 enum), no way to force a specific named encoder component (e.g. `c2.android.avc.encoder` vs `OMX.amlogic.video.encoder.avc`). Needs adding; check whether the `scrcpykt` dependency's `video {}` builder even exposes an `encoder(name)` call (equivalent of CLI `--video-encoder`) before assuming this is a one-line change.
- `core/domain/.../usecase/scrcpy/LaunchScrcpyUseCase.kt` - launch path. Already has defensive patterns worth reusing for the profiler: `killStaleScrcpyServer()` (pkill leftover `app_process.*scrcpy` on device, bounded 3s), `LAUNCH_TIMEOUT_MS = 15_000L` wrapping the blocking `client.mirror{}` call. No settle-delay/pacing logic exists yet - this is the actual missing piece from the root-cause fix.
- `DeviceSettings.kt` - per-device settings keyed by `deviceId`, already has a `scrcpyOptions` override slot. A profiler-derived custom profile should write here (or a new sibling field) rather than inventing new persistence.
- `ScrcpyNewDisplayProfileRepositoryImpl.kt` - existing repository pattern to mirror for a new `DeviceProfileRepository` if a separate profile model is preferred over reusing `DeviceSettings.scrcpyOptions` directly.

## Proposed profiler design (not yet built)

1. New `DeviceProfile` model: `videoEncoder`, `safeBitRate`, `safeMaxFps`, `settleDelayMs`. Persist per device serial.
2. `ProfileDeviceUseCase`, headless (no window):
   - `adb shell getprop` + `scrcpy --list-encoders` → candidate list, hw first.
   - Per candidate: paced probe (connect → sleep settle → ping → sleep → headless `mirror{ noPlayback }` bounded by timeout) → check adb responsiveness after → reuse `killStaleScrcpyServer()` on failure.
   - First surviving encoder wins; hw fail → try sw; sw fail → mark unsupported.
   - Bitrate ramp on the winning encoder (2M→4M→6M→8M→12M), same probe pattern, take highest stable step, back off one notch for margin.
3. Surface as 4th tier option ("Custom (profiled)") in `ScrcpyTierDialog.kt` once a profile exists for the connected device.
4. Every probe step needs its own bounded timeout - a frozen device during profiling must not hang the profiler itself (same discipline as the existing `LAUNCH_TIMEOUT_MS` / stale-server-kill in `LaunchScrcpyUseCase.kt`).

**Open question before implementation**: confirm `scrcpykt`'s video option builder supports specifying a named encoder (not just codec family). If it doesn't, the encoder-selection half of this plan needs a workaround (e.g. shelling out to raw `scrcpy` binary instead of the library for the profiler specifically, or patching the dependency).
