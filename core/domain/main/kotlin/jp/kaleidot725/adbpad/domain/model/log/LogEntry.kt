package jp.kaleidot725.adbpad.domain.model.log

enum class LogLevel(
    val letter: Char,
) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    FATAL('F'),
    ;

    companion object {
        fun fromLetter(letter: Char): LogLevel? =
            when (letter) {
                'A' -> FATAL
                else -> entries.firstOrNull { it.letter == letter }
            }
    }
}

/**
 * One logcat line in `-v time` format, e.g.
 * `09-25 12:34:56.789 W/ActivityManager( 1234): Slow operation`.
 * Lines that don't match (e.g. "--------- beginning of main") keep [level] null and the raw text as [message].
 */
data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel?,
    val tag: String,
    val pid: String,
    val message: String,
    val raw: String,
) {
    fun matches(query: String): Boolean =
        query.isBlank() ||
            tag.contains(query, ignoreCase = true) ||
            message.contains(query, ignoreCase = true) ||
            pid == query.trim()

    companion object {
        private val TIME_FORMAT = Regex("""^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})\s+([VDIWEFA])/(.*?)\(\s*(\d+)\):\s?(.*)$""")

        fun parse(
            id: Long,
            line: String,
        ): LogEntry {
            val match = TIME_FORMAT.matchEntire(line)
            if (match == null) {
                return LogEntry(id = id, time = "", level = null, tag = "", pid = "", message = line, raw = line)
            }
            val (time, level, tag, pid, message) = match.destructured
            return LogEntry(
                id = id,
                time = time,
                level = LogLevel.fromLetter(level.first()),
                tag = tag.trim(),
                pid = pid,
                message = message,
                raw = line,
            )
        }
    }
}
