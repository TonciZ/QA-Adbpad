package jp.kaleidot725.adbpad.domain.usecase.command

import jp.kaleidot725.adbpad.domain.model.command.CommandExecutionHistory
import jp.kaleidot725.adbpad.domain.model.command.CustomCommandParser
import jp.kaleidot725.adbpad.domain.repository.CustomCommandRepository
import jp.kaleidot725.adbpad.domain.repository.NormalCommandOutputRepository
import kotlinx.coroutines.CancellationException

class ExecuteCustomCommandUseCase(
    private val customCommandRepository: CustomCommandRepository,
    private val normalCommandOutputRepository: NormalCommandOutputRepository,
) {
    /**
     * Runs every line of [input] as an adb command, in order, stopping at the first failure.
     * [selectedSerial] is asked per line because the selection can change while lines run.
     */
    suspend operator fun invoke(
        input: String,
        selectedSerial: () -> String?,
    ) {
        val commands = CustomCommandParser.parse(input)
        if (commands.isEmpty()) return

        val transcript = Transcript()
        // After `adb connect <ip>` the following lines are meant for that device, even when
        // another one is currently selected in the app or the device list has not refreshed yet.
        var connectedSerial: String? = null

        try {
            for (command in commands) {
                val args = CustomCommandParser.withDevice(command, connectedSerial ?: selectedSerial())
                transcript.startCommand(CustomCommandParser.display(args))
                publish(transcript)

                val output = StringBuilder()
                val exitCode =
                    try {
                        customCommandRepository.execute(args) { chunk ->
                            output.append(chunk)
                            transcript.append(chunk)
                            publish(transcript)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        transcript.appendLine("Exception: ${e.message}")
                        publish(transcript)
                        return
                    }

                val connectTarget = CustomCommandParser.connectTarget(command)
                val connectFailed = connectTarget != null && !output.contains("connected to")
                if (exitCode != 0 || connectFailed) {
                    if (exitCode != 0) transcript.appendLine("[exit code $exitCode]")
                    publish(transcript)
                    return
                }
                if (connectTarget != null) connectedSerial = connectTarget
            }
        } catch (e: CancellationException) {
            transcript.appendLine("[stopped]")
            publish(transcript)
            throw e
        }
    }

    private suspend fun publish(transcript: Transcript) {
        normalCommandOutputRepository.setExecutionHistory(transcript.toHistory())
    }

    private class Transcript {
        private var firstCommand: String? = null
        private val body = StringBuilder()

        fun startCommand(display: String) {
            if (firstCommand == null) {
                firstCommand = "$PROMPT$display"
            } else {
                ensureNewLine()
                body.append("\n$PROMPT$display\n")
            }
        }

        fun append(text: String) {
            body.append(text)
            // ponytail: this pane is not a log viewer - keep the tail so a runaway command
            // (`adb logcat`) cannot grow the text without bound.
            if (body.length > MAX_OUTPUT_CHARS) {
                val cut = body.indexOf("\n", body.length - MAX_OUTPUT_CHARS)
                body.delete(0, if (cut >= 0) cut + 1 else body.length - MAX_OUTPUT_CHARS)
            }
        }

        fun appendLine(text: String) {
            ensureNewLine()
            body.append(text).append('\n')
        }

        fun toHistory() = CommandExecutionHistory(command = firstCommand.orEmpty(), output = body.toString().trimEnd())

        private fun ensureNewLine() {
            if (body.isNotEmpty() && !body.endsWith('\n')) body.append('\n')
        }
    }

    companion object {
        const val PROMPT = "$ "
        private const val MAX_OUTPUT_CHARS = 50_000
    }
}
