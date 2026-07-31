package jp.kaleidot725.adbpad.domain.model.screenshot

import java.io.File

data class Screenshot(
    val file: File?,
    val deviceLabel: String? = null,
) {
    companion object {
        val EMPTY = Screenshot(null)
    }
}
