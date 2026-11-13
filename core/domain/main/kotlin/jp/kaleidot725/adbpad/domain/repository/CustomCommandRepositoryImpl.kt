package jp.kaleidot725.adbpad.domain.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.concurrent.thread

class CustomCommandRepositoryImpl(
    private val settingRepository: SettingRepository,
) : CustomCommandRepository {
    // ponytail: the real adb binary, not the adam library - `adb connect`, `adb devices` and
    // friends are client-side commands, and users paste lines written for the real CLI.
    override suspend fun execute(
        args: List<String>,
        onOutput: suspend (String) -> Unit,
    ): Int =
        withContext(Dispatchers.IO) {
            val process =
                AdbBinary
                    .processBuilder(settingRepository, *args.toTypedArray())
                    .redirectErrorStream(true)
                    .start()
            val chunks = Channel<String>(CHUNK_BUFFER)
            try {
                // No stdin: an interactive command (`adb shell` with no arguments) sees EOF
                // and exits instead of waiting forever for input nobody can type.
                process.outputStream.close()

                // A blocking pipe read cannot be cancelled, so it lives on its own daemon
                // thread and the coroutine side only ever waits on the channel.
                thread(isDaemon = true, name = "adb-custom-command-output") {
                    try {
                        process.inputStream.reader(Charsets.UTF_8).use { reader ->
                            val buffer = CharArray(READ_BUFFER)
                            while (true) {
                                val read = reader.read(buffer)
                                if (read < 0) break
                                if (chunks.trySendBlocking(String(buffer, 0, read)).isFailure) break
                            }
                        }
                    } catch (_: IOException) {
                        // stream closed because the process was killed
                    } finally {
                        chunks.close()
                    }
                }

                coroutineScope {
                    val pump = launch { for (chunk in chunks) onOutput(chunk.replace("\r", "")) }
                    val exitCode = runInterruptible { process.waitFor() }
                    // ponytail: a command that makes adb spawn its server (`start-server`) leaves
                    // the daemon holding our pipe, so EOF may never come - stop waiting for it.
                    withTimeoutOrNull(DRAIN_TIMEOUT) { pump.join() } ?: pump.cancel()
                    exitCode
                }
            } finally {
                chunks.cancel()
                process.destroyForcibly()
            }
        }

    companion object {
        private const val READ_BUFFER = 8192
        private const val CHUNK_BUFFER = 16
        private const val DRAIN_TIMEOUT = 1_000L
    }
}
