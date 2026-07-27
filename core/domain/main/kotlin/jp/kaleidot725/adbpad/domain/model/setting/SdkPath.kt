package jp.kaleidot725.adbpad.domain.model.setting

import kotlinx.serialization.Serializable

@Serializable
data class SdkPath(
    val adbDirectory: String = "",
    // ponytail: standard adb server port. scrcpy shells out to `adb` internally with no way
    // for us to pass a custom port through, so a non-default value here can never fully
    // propagate - defaulting to what every adb-based tool already assumes with zero config
    // is what actually makes wireless ADB/scrcpy/screenshots work out of the box.
    val adbServerPort: Int = 5037,
)
