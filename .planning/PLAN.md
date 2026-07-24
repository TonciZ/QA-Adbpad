# ADBpad Fork — Feature Plan

Fork: `https://github.com/TonciZ/adbpad`
Local: `D:\Documents\ClaudeCode\Projects\adbpad`
Base: `https://github.com/kaleidot725/adbpad` v3.9.0

## Feature 1: Fix Android TV Screenshot

**Problem:** Screenshot hangs infinitely on Android TV devices. The `cmd uimode night yes/no` shell command (used by Both/Dark/Light modes) likely blocks forever on TV — TV doesn't support UiMode night toggle. `ScreenCaptureRequest` may also hang on some TV firmware.

**Root cause location:** `ScreenshotCommandRepositoryImpl.kt` lines 107-155 (theme commands) and line 165 (`ScreenCaptureRequest`).

**Fix:**

1. Add `withTimeout(10_000)` around the `adb.execute(ScreenCaptureRequest(...))` call in `capture()` (line 165)
2. Add `withTimeout(5_000)` around `sendCommand()` calls in `sendBothCommand/sendDarkCommand/sendLightCommand`
3. On timeout in `capture()`, fall back to `adb shell screencap -p` via adam's `ShellCommandRequest`, pipe bytes to file
4. On timeout in `sendCommand()`, return false (triggers `onFailed`)

**Files:**

- `core/domain/main/kotlin/.../repository/ScreenshotCommandRepositoryImpl.kt` — add timeouts + fallback

**Verification:** Build, run against Android TV. "Current" mode should succeed. Theme modes should fail gracefully (not hang).

---

## Feature 2: Wireless ADB Connection

**Renamed:** "Wireless ADB" (not "Connect WiFi")

**What:** UI to pair and connect ADB over WiFi. No shell subprocess — adam library has `ConnectDeviceRequest(host, port)`, `PairDeviceRequest(host, port, code)`, `DisconnectDeviceRequest(host, port)`.

**Architecture (follows existing patterns):**

### Domain layer (`:core:domain`)

- New `DeviceConnectionRepository` interface:
  - `suspend fun connectDevice(host: String, port: Int): String`
  - `suspend fun pairDevice(host: String, port: Int, code: String): String`
  - `suspend fun disconnectDevice(host: String, port: Int): String`
- New `DeviceConnectionRepositoryImpl` using adam's request classes
- New use cases: `ConnectDeviceUseCase`, `PairDeviceUseCase`, `DisconnectDeviceUseCase`

### UI layer (`main`)

- Add "Wireless ADB" button in `TopSection.kt` (next to Scrcpy button)
- New `WirelessAdbDialog.kt` — dialog with:
  - IP address text field
  - Port text field (default 5555 for connect, 5-digit for pair)
  - Pairing code field (shown in pair mode)
  - Connect / Pair / Disconnect buttons
  - Status text showing result
- New `TopAction` entries: `OpenWirelessAdb`, `ConnectWirelessAdb`, `PairWirelessAdb`, `DisconnectWirelessAdb`, `CloseWirelessAdb`
- TopStateHolder wired to use cases

### DI (`main/di/`)

- `repositoryModule` — add `DeviceConnectionRepository` binding
- `domainModule` — add use case factories

### Strings

- `Language` / `EnglishResources` — add wireless ADB strings

**Files:**

- New: `core/domain/main/kotlin/.../repository/DeviceConnectionRepository.kt` (interface)
- New: `core/domain/main/kotlin/.../repository/DeviceConnectionRepositoryImpl.kt`
- New: `core/domain/main/kotlin/.../usecase/device/ConnectDeviceUseCase.kt`
- New: `core/domain/main/kotlin/.../usecase/device/PairDeviceUseCase.kt`
- New: `core/domain/main/kotlin/.../usecase/device/DisconnectDeviceUseCase.kt`
- New: `main/kotlin/.../ui/section/top/component/WirelessAdbDialog.kt`
- Edit: `main/kotlin/.../ui/section/top/state/TopAction.kt` — add actions
- Edit: `main/kotlin/.../ui/section/top/state/TopState.kt` — add dialog state
- Edit: `main/kotlin/.../ui/section/top/TopStateHolder.kt` — handle actions
- Edit: `main/kotlin/.../ui/section/top/TopSection.kt` — add button + dialog
- Edit: `main/kotlin/.../di/RepositoryModule.kt`
- Edit: `main/kotlin/.../di/DomainModule.kt`
- Edit: `core/domain/main/kotlin/.../model/language/Language.kt`
- Edit: `core/domain/main/kotlin/.../model/language/resources/EnglishResources.kt`

---

## Feature 3: Toggle Commands (with device state query)

**Problem:** Commands like WiFi On/Off, Dark Theme On/Off etc. are separate buttons. User can't tell current state, may toggle ON something already ON.

**Approach:** Merge On/Off pairs into single toggle items. Query device state on device selection and after each toggle.

### New model: `ToggleCommandGroup`

Each group defines:

- `title: String` — display name (e.g., "Wi-Fi")
- `queryCommand: String` — shell command to read current state (e.g., `settings get global wifi_on`)
- `parseState: (String) -> Boolean` — parse query output to on/off
- `onCommand: NormalCommand` — command to turn ON
- `offCommand: NormalCommand` — command to turn OFF
- `isOn: Boolean?` — current state (null = unknown/querying)
- `category: NormalCommandCategory`

### Toggle pairs to define:

| Toggle           | Query command                                                       | ON value                   |
| ---------------- | ------------------------------------------------------------------- | -------------------------- |
| Pointer Location | `settings get system pointer_location`                              | "1"                        |
| Layout Borders   | `getprop debug.layout`                                              | "true"                     |
| Tap Effect       | `settings get system show_touches`                                  | "1"                        |
| Sleep Mode Off   | `settings get global stay_on_while_plugged_in`                      | != "0"                     |
| Dark Theme       | `cmd uimode night`                                                  | contains "Night mode: yes" |
| Wi-Fi            | `settings get global wifi_on`                                       | "1"                        |
| Mobile Data      | `svc data` (no clean query — use `settings get global mobile_data`) | "1"                        |
| Airplane Mode    | `settings get global airplane_mode_on`                              | "1"                        |
| Bluetooth        | `settings get global bluetooth_on`                                  | "1"                        |
| Location         | `settings get secure location_mode`                                 | != "0"                     |
| Animations       | `settings get global window_animation_scale`                        | != "0" / != "0.0"          |
| Auto Rotate      | `settings get system accelerometer_rotation`                        | "1"                        |
| RTL Layout       | `settings get global debug.force_rtl`                               | "1"                        |
| Battery Saver    | `settings get global low_power`                                     | "1"                        |
| Data Saver       | `cmd netpolicy get restrict-background`                             | contains "enabled"         |
| Doze Mode        | `dumpsys deviceidle enabled`                                        | contains "1"               |

Non-toggle commands (font scale, screen rotation, timezone, navigation type) stay as-is — they're multi-option selectors, not toggles.

### State query flow:

1. On device selection → query all toggle states via batch shell commands
2. After toggle execution → re-query that specific toggle's state
3. Show `Switch` composable instead of "Execute" button in card/list view

### UI change:

- `CommandItemCard.kt` / `CommandItemList.kt` — detect toggle vs. non-toggle
  - Toggle: show `Switch` + title (no separate "Execute" button)
  - Non-toggle: keep existing "Execute" button
- `CommandStateHolder.kt` — add `queryDeviceStates(device)` called on device change, `refreshToggleState(toggle)` after execution

**Files:**

- New: `core/domain/main/kotlin/.../model/command/ToggleCommandGroup.kt`
- New: `core/domain/main/kotlin/.../repository/DeviceStateRepository.kt` (interface)
- New: `core/domain/main/kotlin/.../repository/DeviceStateRepositoryImpl.kt`
- New: `core/domain/main/kotlin/.../usecase/command/QueryDeviceStatesUseCase.kt`
- Edit: `core/domain/main/kotlin/.../repository/NormalCommandRepositoryImpl.kt` — return grouped
- Edit: `main/kotlin/.../ui/screen/command/CommandStateHolder.kt` — state query logic
- Edit: `main/kotlin/.../ui/screen/command/state/CommandState.kt` — add toggle states
- Edit: `main/kotlin/.../ui/screen/command/component/CommandItemCard.kt` — toggle switch UI
- Edit: `main/kotlin/.../ui/screen/command/component/CommandItemList.kt` — toggle switch UI
- Edit: `main/kotlin/.../di/RepositoryModule.kt`
- Edit: `main/kotlin/.../di/DomainModule.kt`

---

## Feature 4: Log Capture

**What:** New "Log" screen in nav rail. "Start Logs" begins capturing `adb logcat` output. "Stop & Save" stops capture and saves to file.

**Architecture:**

### Domain layer

- New `LogCaptureRepository` interface:
  - `fun startCapture(device: Device, filter: String = "")`
  - `fun stopCapture(): File` — stops and returns saved file
  - `fun getLogFlow(): Flow<List<String>>` — live log lines
  - `val isCapturing: StateFlow<Boolean>`
- Impl uses adam's `ChanneledShellCommandRequest("logcat")` or `ShellCommandRequest` in a streaming loop

### UI layer

- New `MainCategory.Log` in enum
- New `NavigationRail` entry with `Lucide.ScrollText` icon
- New screen: `LogScreen.kt` + `LogStateHolder.kt` + `LogState.kt` / `LogAction.kt` / `LogSideEffect.kt`
- UI:
  - Top bar: "Start Logs" / "Stop & Save" button, optional filter text field
  - Main area: scrolling log text (monospace, auto-scroll)
  - Saved logs list in left pane (like screenshot list)
- Wire into `MainScreen.kt` as new `CategoryContentLayer`

### DI

- Add `LogCaptureRepository` and `LogStateHolder` bindings

**Files:**

- New: `core/domain/main/kotlin/.../model/MainCategory.kt` — add `Log`
- New: `core/domain/main/kotlin/.../repository/LogCaptureRepository.kt` (interface)
- New: `core/domain/main/kotlin/.../repository/LogCaptureRepositoryImpl.kt`
- New: `core/domain/main/kotlin/.../usecase/log/StartLogCaptureUseCase.kt`
- New: `core/domain/main/kotlin/.../usecase/log/StopLogCaptureUseCase.kt`
- New: `core/domain/main/kotlin/.../usecase/log/GetLogFlowUseCase.kt`
- New: `main/kotlin/.../ui/screen/log/LogScreen.kt`
- New: `main/kotlin/.../ui/screen/log/LogStateHolder.kt`
- New: `main/kotlin/.../ui/screen/log/state/LogState.kt`
- New: `main/kotlin/.../ui/screen/log/state/LogAction.kt`
- New: `main/kotlin/.../ui/screen/log/state/LogSideEffect.kt`
- Edit: `core/domain/main/kotlin/.../model/MainCategory.kt` — add `Log`
- Edit: `core/view/main/kotlin/.../component/rail/NavigationRail.kt` — add Log entry
- Edit: `main/kotlin/.../ui/screen/main/MainScreen.kt` — add logContent slot + CategoryContentLayer
- Edit: `main/kotlin/.../ui/screen/main/MainStateHolder.kt` — create LogStateHolder
- Edit: `main/kotlin/.../di/RepositoryModule.kt`
- Edit: `main/kotlin/.../di/DomainModule.kt`
- Edit: `main/kotlin/.../di/StateHolderModule.kt`
- Edit: Language/EnglishResources — log strings

---

## Execution Order

1. **Feature 1** (Screenshot fix) — smallest, unblocks Android TV usage
2. **Feature 2** (Wireless ADB) — standalone, no dependencies on other features
3. **Feature 4** (Log Capture) — new screen, independent
4. **Feature 3** (Toggle Commands) — biggest refactor, touches existing command system

Each feature: implement → compile check → commit.
Final: build distributable, test on devices.
