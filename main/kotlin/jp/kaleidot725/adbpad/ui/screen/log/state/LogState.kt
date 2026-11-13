package jp.kaleidot725.adbpad.ui.screen.log.state

import jp.kaleidot725.adbpad.domain.model.log.LogEntry
import jp.kaleidot725.adbpad.domain.model.log.LogLevel
import jp.kaleidot725.pulse.mvi.PulseState

data class LogState(
    val entries: List<LogEntry> = emptyList(),
    val isCapturing: Boolean = false,
    val hasDevice: Boolean = false,
    val filter: String = "",
    val savedFile: String = "",
    val search: String = "",
    val minLevel: LogLevel = LogLevel.VERBOSE,
    val autoScroll: Boolean = true,
    val wrapLines: Boolean = true,
    val saveDialog: LogSaveDialogState? = null,
) : PulseState {
    val isFiltered: Boolean get() = search.isNotBlank() || minLevel != LogLevel.VERBOSE

    // Unparsed lines (level == null) are always kept so headers/crash dumps don't vanish under a level filter.
    val visibleEntries: List<LogEntry> by lazy {
        if (!isFiltered) {
            entries
        } else {
            entries.filter { entry ->
                (entry.level?.let { it >= minLevel } ?: true) && entry.matches(search)
            }
        }
    }
}

data class LogSaveDialogState(
    val fileName: String,
    val directory: String,
    val totalLines: Long,
    val shownLines: Int,
    // Offered only while a search or level filter is narrowing the view.
    val canSaveShownOnly: Boolean,
    val onlyShown: Boolean = false,
    val failed: Boolean = false,
)
