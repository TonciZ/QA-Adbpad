package jp.kaleidot725.adbpad.ui.screen.log.state

import jp.kaleidot725.adbpad.domain.model.log.LogEntry
import jp.kaleidot725.adbpad.domain.model.log.LogLevel
import jp.kaleidot725.pulse.mvi.PulseState

data class LogState(
    val entries: List<LogEntry> = emptyList(),
    val isCapturing: Boolean = false,
    val filter: String = "",
    val savedFile: String = "",
    val search: String = "",
    val minLevel: LogLevel = LogLevel.VERBOSE,
    val autoScroll: Boolean = true,
    val wrapLines: Boolean = true,
) : PulseState {
    // Unparsed lines (level == null) are always kept so headers/crash dumps don't vanish under a level filter.
    val visibleEntries: List<LogEntry> by lazy {
        if (search.isBlank() && minLevel == LogLevel.VERBOSE) {
            entries
        } else {
            entries.filter { entry ->
                (entry.level?.let { it >= minLevel } ?: true) && entry.matches(search)
            }
        }
    }
}
