# QA-Adbpad - Changes from upstream AdbPad

**TL;DR:** This fork exists because the upstream app didn't reliably work against Android TV
devices - screenshots silently failed and scrcpy mirroring crashed or froze the TV. Log capture
and one-click toggle commands were added as QA quality-of-life on top of that.

Fork of [AdbPad](https://github.com/kaleidot725/adbpad) by [kaleidot725](https://github.com/kaleidot725),
renamed to distinguish it from upstream since the changes below are Android-TV-focused and not
upstreamed.

## Why this fork exists

The original app targets phones/tablets. Testing against Android TV boxes (common in QA
crowdtesting work) exposed bugs upstream doesn't hit on mobile: screenshot capture that silently
returned empty files, and scrcpy sessions that could freeze a TV box outright. Both needed fixing
before this tool was usable for TV device testing.

## What changed and why

### 1. Screenshot capture rewritten to shell out to real `adb`

**Problem:** Screenshots worked on phones but silently failed on Android TV - no error, no file,
spinner just hung.

**Root cause:** The app used `com.malinskiy.adam`, a pure-Kotlin reimplementation of the ADB
wire protocol, for capture. Two separate bugs in it surfaced only against Android TV hardware:

- The raw-framebuffer capture path (`ScreenCaptureRequest`) only caught `TimeoutCancellationException`.
  When the TV's daemon threw a different exception instead of timing out, it went uncaught and
  killed the coroutine - no fallback ever ran, no error surfaced.
- The fallback path (`screencap` + `PullRequest`) trusted the device's advertised
  `host-serial:features` list. Android TV boxes commonly advertise `SENDRECV_V2` support their
  daemon doesn't actually implement correctly - `PullRequest` reported `pulled=true` while writing
  zero bytes to disk.

**Fix:** Verified manually with the real `adb.exe` CLI that `adb exec-out screencap -p` works
correctly against the affected TV. Replaced the entire adam-based capture path with a direct
`ProcessBuilder` call to the real `adb` binary, streaming its stdout straight to the screenshot
file. Simpler and no longer dependent on adam's protocol reimplementation being correct.

See: `core/domain/main/kotlin/jp/kaleidot725/adbpad/domain/repository/ScreenshotCommandRepositoryImpl.kt`

### 2. Screenshot UI simplified to one button

**Problem:** The original UI let you pick "capture in light theme / dark theme / both / current"
via a dropdown. The default selection (`Both`) toggled the device's dark-theme setting via a shell
`settings put` command before capturing - which fails silently on devices without
`WRITE_SECURE_SETTINGS`, so the very first capture attempt on a fresh device was doomed regardless
of the adam bugs above.

**Fix:** Removed the entire theme-switching command abstraction (`ScreenshotCommand`
Light/Dark/Both/Current, the dropdown menu, `GetScreenshotCommandUseCase`) since we only need a
plain screenshot for QA purposes. One camera-icon button, no theme toggling, no permission
dependency.

### 3. Scrcpy defaults changed to actually launch on constrained hardware

**Problem:** Scrcpy mirroring crashed on a phone and wouldn't launch at all on the Android TV box;
repeated attempts froze the TV outright.

**Root causes (two, compounding):**

- `ScrcpyOptions.noAudio` defaulted to `false`. Audio capture requires Android 11+ and specific
  device support - attempting it is the single most common scrcpy crash cause on older phones and
  most Android TV boxes.
- Repeatedly force-killing the app during testing (`taskkill /F`) killed the scrcpy _client_
  without giving it a chance to tell the device-side `scrcpy-server` to shut down. An orphaned
  server left holding the display/encoder, combined with a second capture session starting on top
  of it, hung the TV box's SoC until a hard reboot.

**Fix:**

- `ScrcpyOptions.noAudio` now defaults to `true`.
- `LaunchScrcpyUseCase` now kills any leftover `scrcpy-server` process on the device (`adb shell
pkill -f app_process.*scrcpy`) before every launch, so a stale session can't collide with a new
  one.

See: `core/domain/main/kotlin/jp/kaleidot725/adbpad/domain/model/device/ScrcpyOptions.kt`,
`core/domain/main/kotlin/jp/kaleidot725/adbpad/domain/usecase/scrcpy/LaunchScrcpyUseCase.kt`

### 4. Log capture (new)

**Why:** QA testing needs `logcat` output tied to a repro, not a manual terminal session.

Added a Logs screen (nav rail entry) with Start / Stop / Save, backed by `LogCaptureRepository`
shelling out to `adb logcat` directly (same real-binary approach as the screenshot fix, not adam).

### 5. Wireless ADB connection dialog (new)

**Why:** Android TV boxes are almost always connected wirelessly, not via USB - needed a way to
connect/pair without dropping to a terminal.

Added a dialog for IP/port/pairing-code entry, wired to `ConnectDeviceUseCase` /
`PairDeviceUseCase` / `DisconnectDeviceUseCase`.

### 6. Toggle commands (new, quality of life)

**Why:** Roughly half of AdbPad's ~40 device commands are On/Off pairs (Wi-Fi, Bluetooth,
Airplane mode, Dark theme, etc.) that were previously two separate buttons in the command grid,
with no indication of current device state.

Added `ToggleCommandDef` - 16 On/Off pairs collapsed into a single Material3 `Switch` per command,
with a device-state query (`queryShell`) run on device selection so the switch reflects reality
rather than assuming state. Optimistic UI update on toggle, confirmed/reverted by re-querying the
device after the command executes.

### 7. Android TV screenshot hang fix (earlier pass, superseded by #1)

An initial pass added a 10s timeout + `screencap -p` shell fallback around the adam-based capture
to stop it hanging indefinitely on Android TV. That fallback is what exposed the `PullRequest`
silent-failure bug described in #1, which is why capture was rewritten to bypass adam entirely
rather than patching the fallback further.

### 8. Device liveness check + real-reboot restart button

**Why:** `adb devices` only reports whether the ADB link is up, not whether the device is actually
responsive - a device can report `device` while completely frozen, which is exactly what happened
with the Android TV box under test.

Added an active liveness probe (`adb shell echo` with a 5s timeout) that runs automatically on
device selection and on demand via a heart-pulse button, plus a restart button that shells out to
a real `adb reboot` (not the existing soft power keyevent, which goes through the same UI input
pipe that's frozen when a device hangs). A status dot next to the two buttons shows red (adb link
down), gray (checking/unknown), green (responsive), or yellow (connected but unresponsive).

### 9. Systemic ADB server port mismatch fix

**Problem:** The app's own ADB server ran on a non-standard port (30000) that only
`StartAdbUseCase` knew about; every other ADB-consuming code path (the adam client repos, and raw
`adb` CLI shellouts) defaulted to the standard 5037, so they silently couldn't reach the app's own
server unless something else happened to have a server running on 5037.

**Fix:** Default server port changed 30000 → 5037 (standard, also required because scrcpy has no
API to pass a custom ADB server port). Added an `AdbBinary` helper so every raw `adb` CLI shellout
uses the configured port consistently. The adam-based repos (Device, DeviceConnection,
DeviceControlCommand, NormalCommand) now build their client per-call from the configured port
instead of a hardcoded default.

### 10. Scrcpy quality tier picker (new)

**Why:** Scrcpy freezing on weaker Android TV boxes was partly caused by uncapped/near-native
video defaults overloading a weak encoder.

Added a Low/Mid/High mirror-quality dialog shown on every scrcpy launch, so the right defaults get
picked per device instead of always maxing out. Presets are persisted via `SettingRepository` and
editable in Settings > SDK > Scrcpy Quality Presets. `LaunchScrcpyUseCase` accepts an optional tier
override, with a "Use saved device settings" skip on the dialog.

### 11. Scrcpy auto-profiler for problem encoders (new)

**Problem:** Even with the tier picker, one specific Android TV box (Amlogic SoC) still froze on
every scrcpy connect while its physical screen was on - reproduced 3/3 times regardless of
bitrate/resolution/profile tuning.

**Root cause:** The vendor's hardware h264 encoder (`OMX.amlogic.video.encoder.avc`) can't handle
an immediate connection right after `adb connect`/app launch - it needs the connection paced
(settle delay, then a readiness ping, before the mirror session opens). Confirmed with raw
`scrcpy` CLI testing outside the app: paced connects succeeded 4/4 at a moderate bitrate ceiling;
unpaced connects froze the TV every time regardless of bitrate/profile tuning.

**Fix:** Added a device scan (Settings dialog "Scan device for a working profile") that probes the
device's available h264 encoders (hw first, then sw fallback, parsed from `scrcpy
--list-encoders`) with a paced connection, then ramps the bitrate up and backs off one notch from
the highest surviving step. The resulting profile (encoder + safe bitrate/fps + settle delay) is
saved per device and surfaced as a "Custom (profiled)" tier. Every scrcpy launch now also flushes
on-device stale state (kills any leftover `scrcpy-server` process, clears `adb reverse`/`forward`
tunnels) before connecting, since a killed session can leave the next connection's control channel
wedged even though `adb` itself stays responsive.

See: `core/domain/main/kotlin/jp/kaleidot725/adbpad/domain/usecase/scrcpy/ProfileDeviceUseCase.kt`,
`core/domain/main/kotlin/jp/kaleidot725/adbpad/domain/usecase/scrcpy/LaunchScrcpyUseCase.kt`

### 12. Windows device-settings persistence bug fix

**Problem:** Per-device settings (custom name, scrcpy options, the new profiler output) never
actually persisted to disk for any wireless-adb device on Windows - saves silently failed every
time.

**Root cause:** The settings filename was built directly from the device serial
(`device_<serial>.json`). A wireless-adb serial looks like `192.168.1.227:5555` - the `:` is a
legal adb serial character but an illegal Windows filename character, so every write threw
`IOException` (caught, silently returned `false`).

**Fix:** Sanitize the device ID before it's used in any filename.

### 13. Check for Updates (new)

**Why:** No way to know a new version existed short of manually checking GitHub.

Added a Settings > Updates pane that checks the latest GitHub release tag against the running
version, and if newer, downloads the matching installer asset and launches it (`msiexec` on
Windows, `open` on Mac), then quits so the installer can replace files cleanly. Not a silent
in-place patch - jpackage-built installers have no binary-diff/patch mechanism - but it removes the
manual find-download-run steps.

### 14. Log save-location picker

**Why:** Log capture always wrote to the fixed app-data `logs/` folder with no way to choose where
a specific capture landed.

`LogCaptureRepository.stopCapture()` now opens a native "Save As" dialog every time capture stops
(defaulting to the old folder + a timestamped filename) instead of writing there unconditionally.

### 15. "Failed to launch JVM" on machines with an accessibility flag enabled

**Problem:** The packaged app refused to start at all on some Windows machines, showing
jpackage's generic "Failed to launch JVM" dialog - reproducible even on a clean uninstall +
reinstall, so not an install-corruption issue.

**Root cause:** On Windows machines with an accessibility/screen-reader flag enabled (Ease of
Access), AWT tries to load `com.sun.java.accessibility.AccessBridge` at `Toolkit` init. The
packaged runtime's module list (a custom, hand-picked set for a smaller installer) didn't include
`jdk.accessibility`, so that class doesn't exist in the bundled JVM - `ClassNotFoundException` →
uncaught `AWTError` → the process dies before any window appears, which jpackage's native launcher
reports as the generic "Failed to launch JVM".

**Fix:** Added `jdk.accessibility` to the packaged runtime's module list.

### 16. Screenshot tab: newest-first, bulk delete, device label

**Why:** With multiple devices (phone, Android TV, Fire TV) captured into the same flat
screenshot folder, the list was hard to manage - oldest-first ordering buried new captures at the
bottom, there was no way to clear out a batch of screenshots at once, and nothing on a row said
which device it came from.

- Default sort flipped to newest-first (captured screenshots now appear at the top of the list).
- Added multi-select checkboxes per row, a header "select all" checkbox, and a bulk-delete button
  that appears once at least one screenshot is selected.
- Each screenshot filename now embeds the capturing device's name (or serial/IP for unnamed
  devices), shown in the list as part of the row's detail line. Older screenshots captured before
  this change simply show no device label.

### 17. Fix Check for Updates hanging forever

**Problem:** Reported on 1.3.1 - the "Check for Updates" button could spin forever and never
resolve.

**Root cause:** Confirmed out-of-app before touching any code: the packaged runtime's hand-picked
module list (`build.gradle.kts` `nativeDistributions.modules(...)`) never included `java.net.http`.
Built a `jlink` image with the exact same module list and ran the check's `HttpClient` call
against it - `HttpClient.newHttpClient()` threw `NoClassDefFoundError` immediately, every time.
`NoClassDefFoundError` is an `Error`, not an `Exception`, so the use case's
`catch (_: Exception)` never caught it - it killed the coroutine before it could ever reset the
"checking..." UI state, leaving the spinner stuck permanently. Confirmed the fix the same way:
rebuilt the `jlink` image with `java.net.http` added and the identical test succeeded.

**Fix:** Added `modules("java.net.http")` to `nativeDistributions`. Also wrapped the whole check
in a coroutine-level `withTimeoutOrNull(15s)` and widened the use case's catch to `Throwable`,
so a stuck DNS resolver or a similar class-loading gap in the future degrades to "up to date"
instead of hanging forever silently again.

### Also

- `org.gradle.toolchains.foojay-resolver-convention` bumped `0.10.0` → `1.0.0` - the old version
  throws `NoSuchFieldError: IBM_SEMERU` against Gradle 9.3.1.
- App renamed AdbPad → **QA-Adbpad** (window title, `rootProject.name`) to distinguish this fork
  from upstream. The Kotlin package namespace (`jp.kaleidot725.adbpad`) was intentionally left
  unchanged - renaming it touches 150+ files for no functional benefit.

## Known limitations

- Virtual Display (`--new-display`) requires Android 14+ on the device and starts genuinely blank
  - nothing is auto-launched onto the new display. This is scrcpy's own behavior, not a bug here.
- Scrcpy stability on Android TV hardware depends on the device's video encoder; the fixes above
  address the crash/freeze causes found on the specific hardware tested against, not every
  possible Android TV chipset quirk.
