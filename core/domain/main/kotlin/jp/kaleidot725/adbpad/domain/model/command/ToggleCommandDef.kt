package jp.kaleidot725.adbpad.domain.model.command

data class ToggleCommandDef(
    val onKey: String,
    val offKey: String,
    val queryCommand: String,
    val parseIsOn: (String) -> Boolean,
    val createOnCommand: () -> NormalCommand,
    val createOffCommand: () -> NormalCommand,
) {
    companion object {
        val ALL: List<ToggleCommandDef> =
            listOf(
                ToggleCommandDef(
                    onKey = NormalCommand.PointerLocationOn().favoriteKey,
                    offKey = NormalCommand.PointerLocationOff().favoriteKey,
                    queryCommand = "settings get system pointer_location",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.PointerLocationOn() },
                    createOffCommand = { NormalCommand.PointerLocationOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.LayoutBorderOn().favoriteKey,
                    offKey = NormalCommand.LayoutBorderOff().favoriteKey,
                    queryCommand = "getprop debug.layout",
                    parseIsOn = { it.trim().equals("true", ignoreCase = true) },
                    createOnCommand = { NormalCommand.LayoutBorderOn() },
                    createOffCommand = { NormalCommand.LayoutBorderOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.TapEffectOn().favoriteKey,
                    offKey = NormalCommand.TapEffectOff().favoriteKey,
                    queryCommand = "settings get system show_touches",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.TapEffectOn() },
                    createOffCommand = { NormalCommand.TapEffectOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.SleepModeOn().favoriteKey,
                    offKey = NormalCommand.SleepModeOff().favoriteKey,
                    queryCommand = "settings get global stay_on_while_plugged_in",
                    parseIsOn = { it.trim() == "0" || it.trim() == "null" },
                    createOnCommand = { NormalCommand.SleepModeOn() },
                    createOffCommand = { NormalCommand.SleepModeOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.DarkThemeOn().favoriteKey,
                    offKey = NormalCommand.DarkThemeOff().favoriteKey,
                    queryCommand = "cmd uimode night",
                    parseIsOn = { it.contains("yes", ignoreCase = true) },
                    createOnCommand = { NormalCommand.DarkThemeOn() },
                    createOffCommand = { NormalCommand.DarkThemeOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.WifiOn().favoriteKey,
                    offKey = NormalCommand.WifiOff().favoriteKey,
                    queryCommand = "settings get global wifi_on",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.WifiOn() },
                    createOffCommand = { NormalCommand.WifiOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.DataOn().favoriteKey,
                    offKey = NormalCommand.DataOff().favoriteKey,
                    queryCommand = "settings get global mobile_data",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.DataOn() },
                    createOffCommand = { NormalCommand.DataOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.AirplaneModeOn().favoriteKey,
                    offKey = NormalCommand.AirplaneModeOff().favoriteKey,
                    queryCommand = "settings get global airplane_mode_on",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.AirplaneModeOn() },
                    createOffCommand = { NormalCommand.AirplaneModeOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.BluetoothOn().favoriteKey,
                    offKey = NormalCommand.BluetoothOff().favoriteKey,
                    queryCommand = "settings get global bluetooth_on",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.BluetoothOn() },
                    createOffCommand = { NormalCommand.BluetoothOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.LocationOn().favoriteKey,
                    offKey = NormalCommand.LocationOff().favoriteKey,
                    queryCommand = "settings get secure location_mode",
                    parseIsOn = { it.trim() != "0" && it.trim() != "null" },
                    createOnCommand = { NormalCommand.LocationOn() },
                    createOffCommand = { NormalCommand.LocationOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.AnimationsOn().favoriteKey,
                    offKey = NormalCommand.AnimationsOff().favoriteKey,
                    queryCommand = "settings get global window_animation_scale",
                    parseIsOn = {
                        val v = it.trim().toFloatOrNull()
                        v != null && v > 0f
                    },
                    createOnCommand = { NormalCommand.AnimationsOn() },
                    createOffCommand = { NormalCommand.AnimationsOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.AutoRotateOn().favoriteKey,
                    offKey = NormalCommand.AutoRotateOff().favoriteKey,
                    queryCommand = "settings get system accelerometer_rotation",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.AutoRotateOn() },
                    createOffCommand = { NormalCommand.AutoRotateOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.RtlLayoutOn().favoriteKey,
                    offKey = NormalCommand.RtlLayoutOff().favoriteKey,
                    queryCommand = "settings get global debug.force_rtl",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.RtlLayoutOn() },
                    createOffCommand = { NormalCommand.RtlLayoutOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.BatterySaverOn().favoriteKey,
                    offKey = NormalCommand.BatterySaverOff().favoriteKey,
                    queryCommand = "settings get global low_power",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.BatterySaverOn() },
                    createOffCommand = { NormalCommand.BatterySaverOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.DataSaverOn().favoriteKey,
                    offKey = NormalCommand.DataSaverOff().favoriteKey,
                    queryCommand = "cmd netpolicy get restrict-background",
                    parseIsOn = { it.contains("true", ignoreCase = true) },
                    createOnCommand = { NormalCommand.DataSaverOn() },
                    createOffCommand = { NormalCommand.DataSaverOff() },
                ),
                ToggleCommandDef(
                    onKey = NormalCommand.DozeModeOn().favoriteKey,
                    offKey = NormalCommand.DozeModeOff().favoriteKey,
                    queryCommand = "dumpsys deviceidle enabled",
                    parseIsOn = { it.trim() == "1" },
                    createOnCommand = { NormalCommand.DozeModeOn() },
                    createOffCommand = { NormalCommand.DozeModeOff() },
                ),
            )

        val OFF_KEYS: Set<String> = ALL.map { it.offKey }.toSet()
        val ON_KEY_MAP: Map<String, ToggleCommandDef> = ALL.associateBy { it.onKey }
    }
}
