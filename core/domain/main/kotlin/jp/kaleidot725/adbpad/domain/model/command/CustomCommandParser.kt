package jp.kaleidot725.adbpad.domain.model.command

/**
 * Turns what a user typed (or pasted) into argument lists for the adb binary.
 *
 * Only adb is ever run: the text is tokenized here and handed to the process as an argument
 * list, never to a host shell, so `&&`, `|` or `>` have no meaning on the PC side.
 */
object CustomCommandParser {
    private const val DEFAULT_ADB_PORT = 5555

    // Commands that talk to the adb server, not to one device - `-s <serial>` is meaningless there.
    private val HOST_COMMANDS =
        setOf(
            "connect",
            "disconnect",
            "pair",
            "devices",
            "kill-server",
            "start-server",
            "version",
            "help",
            "mdns",
            "keygen",
        )

    /** One argument list per non-blank line, with a leading `adb` / `adb.exe` removed. */
    fun parse(input: String): List<List<String>> =
        input
            .lines()
            .map { tokenize(it) }
            .map { tokens -> if (tokens.firstOrNull()?.lowercase() in setOf("adb", "adb.exe")) tokens.drop(1) else tokens }
            .filter { it.isNotEmpty() }

    /**
     * Splits on whitespace, keeping quoted parts together. Both quote styles work because
     * commands get copied from bash and cmd examples alike. A backslash is literal (Windows
     * paths) except for `\"` inside double quotes.
     */
    fun tokenize(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var hasToken = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                quote == null && char.isWhitespace() -> {
                    if (hasToken) tokens.add(current.toString())
                    current.clear()
                    hasToken = false
                }

                quote == null && (char == '"' || char == '\'') -> {
                    quote = char
                    hasToken = true
                }

                quote != null && char == quote -> {
                    quote = null
                }

                quote == '"' && char == '\\' && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }

                else -> {
                    current.append(char)
                    hasToken = true
                }
            }
            index++
        }
        if (hasToken) tokens.add(current.toString())
        return tokens
    }

    /**
     * Adds `-s <serial>` so the command reaches the intended device even when several are
     * connected. Left alone when there is no serial, when the command is server-level, or
     * when the user already passed their own global option (`-s`, `-d`, `-e`, ...).
     */
    fun withDevice(
        args: List<String>,
        serial: String?,
    ): List<String> {
        val first = args.firstOrNull() ?: return args
        if (serial.isNullOrBlank() || first.startsWith("-") || first in HOST_COMMANDS) return args
        return listOf("-s", serial) + args
    }

    /** The serial a `connect <host>[:port]` line produces, or null for any other command. */
    fun connectTarget(args: List<String>): String? {
        if (args.size != 2 || args[0] != "connect") return null
        val target = args[1]
        return if (target.contains(':')) target else "$target:$DEFAULT_ADB_PORT"
    }

    /** The command as a user would type it, for echoing into the output pane. */
    fun display(args: List<String>): String =
        (listOf("adb") + args).joinToString(" ") { arg ->
            if (arg.isEmpty() || arg.any { it.isWhitespace() }) "\"$arg\"" else arg
        }
}
