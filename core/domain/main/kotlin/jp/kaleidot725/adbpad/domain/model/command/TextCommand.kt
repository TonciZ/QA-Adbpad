package jp.kaleidot725.adbpad.domain.model.command

import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TextCommand(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val text: String,
    val option: Option = Option.SendWithTab,
    val isRunning: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
) {
    val requests: List<ShellCommandRequest> get() {
        return buildList {
            val texts = text.replace("\r", "").split('\n')
            texts.forEach { text ->
                if (text.isEmpty()) {
                    add(ShellCommandRequest(""))
                } else {
                    add(ShellCommandRequest("input text ${escapeForInputText(text)}"))
                }
            }
        }
    }

    enum class Option {
        SendWithTab,
        SendWithNewLine,
    }

    /** Characters `adb shell input text` cannot type (it only maps printable ASCII). */
    val unsupportedChars: Set<Char> get() = text.filter { it != '\n' && it != '\r' && (it < ' ' || it > '~') }.toSet()

    companion object {
        // `input text` treats %s as a space and splits on real spaces, and the line runs through the
        // device shell, so quotes, &, ;, |, $, (, ) etc. must not reach it unescaped.
        fun escapeForInputText(line: String): String = "'" + line.replace("'", "'\\''").replace(" ", "%s") + "'"

        fun createNew(
            title: String,
            text: String,
        ): TextCommand = TextCommand(title = title, text = text, isRunning = false)
    }
}
