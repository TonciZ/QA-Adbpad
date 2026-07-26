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
