package jp.kaleidot725.adbpad.data.repository

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import jp.kaleidot725.adbpad.data.local.TextCommandFileCreator
import jp.kaleidot725.adbpad.domain.model.command.KeyCommand
import jp.kaleidot725.adbpad.domain.model.command.TextCommand
import jp.kaleidot725.adbpad.domain.model.device.Device
import jp.kaleidot725.adbpad.domain.repository.SettingRepository
import jp.kaleidot725.adbpad.domain.repository.TextCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TextCommandRepositoryImpl(
    private val settingRepository: SettingRepository,
) : TextCommandRepository {
    private val runningCommands: MutableSet<TextCommand> = mutableSetOf()
    private val lock: Any = Any()

    override suspend fun getAllTextCommand(): List<TextCommand> {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val setting = TextCommandFileCreator.load()
                return@withContext setting.values.map { text ->
                    text.copy(isRunning = runningCommands.any { it.id == text.id })
                }
            }
        }
    }

    override suspend fun addTextCommand(command: TextCommand): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val oldSetting = TextCommandFileCreator.load()
                val newInputTexts = oldSetting.values.toMutableList().apply { add(command) }
                val newSetting = oldSetting.copy(values = newInputTexts)
                return@withContext TextCommandFileCreator.save(newSetting)
            }
        }
    }

    override suspend fun removeTextCommand(command: TextCommand): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val oldSetting = TextCommandFileCreator.load()
                val newInputTexts = oldSetting.values.toMutableList().apply { remove(command) }
                val newSetting = oldSetting.copy(values = newInputTexts)
                return@withContext TextCommandFileCreator.save(newSetting)
            }
        }
    }

    override suspend fun updateTextCommandTitle(
        id: String,
        title: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val oldSetting = TextCommandFileCreator.load()
                val targetIndex = oldSetting.values.indexOfFirst { it.id == id }
                val target = oldSetting.values.getOrNull(targetIndex) ?: return@withContext false
                val newTarget = target.copy(title = title, lastModified = System.currentTimeMillis())
                val newCommands = oldSetting.values.toMutableList()
                newCommands.remove(target)
                newCommands.add(targetIndex, newTarget)

                val newSetting = oldSetting.copy(values = newCommands)
                return@withContext TextCommandFileCreator.save(newSetting)
            }
        }
    }

    override suspend fun updateTextCommandValue(
        id: String,
        value: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val oldSetting = TextCommandFileCreator.load()
                val targetIndex = oldSetting.values.indexOfFirst { it.id == id }
                val target = oldSetting.values.getOrNull(targetIndex) ?: return@withContext false
                val newTarget = target.copy(text = value, lastModified = System.currentTimeMillis())
                val newCommands = oldSetting.values.toMutableList()
                newCommands.remove(target)
                newCommands.add(targetIndex, newTarget)

                val newSetting = oldSetting.copy(values = newCommands)
                return@withContext TextCommandFileCreator.save(newSetting)
            }
        }
    }

    override suspend fun updateTextCommandOption(
        id: String,
        option: TextCommand.Option,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val oldSetting = TextCommandFileCreator.load()
                val targetIndex = oldSetting.values.indexOfFirst { it.id == id }
                val target = oldSetting.values.getOrNull(targetIndex) ?: return@withContext false
                val newTarget = target.copy(option = option, lastModified = System.currentTimeMillis())
                val newCommands = oldSetting.values.toMutableList()
                newCommands.remove(target)
                newCommands.add(targetIndex, newTarget)

                val newSetting = oldSetting.copy(values = newCommands)
                return@withContext TextCommandFileCreator.save(newSetting)
            }
        }
    }

    override suspend fun sendCommand(
        device: Device,
        command: TextCommand,
        onStart: suspend () -> Unit,
        onComplete: suspend () -> Unit,
        onFailed: suspend (reason: String) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            val unsupported = command.unsupportedChars
            if (unsupported.isNotEmpty()) {
                onFailed(
                    "adb 'input text' can only type plain ASCII. Unsupported characters: " +
                        unsupported.joinToString(" "),
                )
                return@withContext
            }

            runningCommands.add(command)
            onStart()

            val failure =
                try {
                    val adbClient = client()
                    delay(300)
                    sendLines(adbClient, device, command)
                } catch (e: Exception) {
                    "${e::class.simpleName}: ${e.message ?: "no message"} (is ${device.serial} still connected?)"
                } finally {
                    runningCommands.remove(command)
                }

            if (failure == null) onComplete() else onFailed(failure)
        }
    }

    /** Returns null on success, or a human-readable reason for the first failing step. */
    private suspend fun sendLines(
        adbClient: AndroidDebugBridgeClient,
        device: Device,
        command: TextCommand,
    ): String? {
        command.requests.forEachIndexed { index, request ->
            if (request.cmd.isNotEmpty()) {
                val result = adbClient.execute(request, device.serial)
                describeFailure(request.cmd, result)?.let { return it }
            }

            if (command.requests.lastIndex != index) {
                val keyCode =
                    when (command.option) {
                        TextCommand.Option.SendWithTab -> 61
                        TextCommand.Option.SendWithNewLine -> 66
                    }

                KeyCommand(keyCode).requests.forEach { keyRequest ->
                    val keyResult = adbClient.execute(keyRequest, device.serial)
                    describeFailure(keyRequest.cmd, keyResult)?.let { return it }
                }
            }
        }
        return null
    }

    // `input` often exits 0 while printing a Java exception, so the output is checked as well.
    private fun describeFailure(
        cmd: String,
        result: ShellCommandResult,
    ): String? {
        val output = result.output.trim()
        val failed = result.exitCode != 0 || output.contains("Exception") || output.startsWith("Error")
        if (!failed) return null
        return buildString {
            append("Line ${line(cmd)} failed (exit code ${result.exitCode})")
            if (output.isNotEmpty()) append(": ").append(output.lineSequence().take(3).joinToString(" | "))
        }
    }

    private fun line(cmd: String): String = if (cmd.length > 60) "\"${cmd.take(57)}...\"" else "\"$cmd\""

    // Built per call so a changed ADB server port in settings takes effect without a restart.
    private suspend fun client(): AndroidDebugBridgeClient =
        AndroidDebugBridgeClientFactory()
            .apply { port = settingRepository.getSdkPath().adbServerPort }
            .build()

    override fun clear() {
        runningCommands.clear()
    }
}
