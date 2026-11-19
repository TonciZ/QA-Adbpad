package jp.kaleidot725.adbpad.data.local

import jp.kaleidot725.adbpad.domain.model.command.DefaultFavorites
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

object NormalCommandFavoriteFileCreator {
    private const val FILE_NAME = "normal_command_favorites.json"

    fun save(favorites: List<String>): Boolean =
        try {
            FilePathUtil.createDir()
            FilePathUtil.getFilePath(FILE_NAME).outputStream().apply {
                this.write(Json.encodeToString(favorites).toByteArray())
                this.close()
            }
            true
        } catch (_: IOException) {
            false
        }

    /**
     * No file yet means favorites were never touched: seed the default pins and write them, so
     * the file exists from then on and a user who unpins everything keeps an empty list.
     */
    fun load(): List<String> =
        try {
            val file = FilePathUtil.getFilePath(FILE_NAME)
            if (!file.exists()) {
                save(DefaultFavorites.KEYS)
                DefaultFavorites.KEYS
            } else {
                Json.decodeFromString(string = file.readText())
            }
        } catch (_: Exception) {
            emptyList()
        }
}
