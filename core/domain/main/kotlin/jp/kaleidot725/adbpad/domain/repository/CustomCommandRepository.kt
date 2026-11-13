package jp.kaleidot725.adbpad.domain.repository

interface CustomCommandRepository {
    /**
     * Runs `adb <args>` with the real adb binary, streaming stdout and stderr (merged) to
     * [onOutput] as it arrives. Returns the exit code. Cancelling the caller kills the process.
     */
    suspend fun execute(
        args: List<String>,
        onOutput: suspend (String) -> Unit,
    ): Int
}
