package jp.kaleidot725.adbpad.ui.screen.app.component.tree

import jp.kaleidot725.adbpad.domain.model.app.AppFileEntry
import jp.kaleidot725.adbpad.domain.model.app.AppFileType

internal val previewAppFileEntries =
    listOf(
        AppFileEntry(
            name = "files",
            path = "/data/data/com.example.notes/files",
            type = AppFileType.Directory,
            permissions = "drwxrwx--x",
            size = 4096,
            date = "2026-05-08",
            time = "12:34",
        ),
        AppFileEntry(
            name = "settings.toml",
            path = "/data/data/com.example.notes/files/settings.toml",
            type = AppFileType.File,
            permissions = "-rw-rw----",
            size = 128,
            date = "2026-05-08",
            time = "12:35",
        ),
    )

