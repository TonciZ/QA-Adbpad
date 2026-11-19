package jp.kaleidot725.adbpad.domain.model.command

/**
 * Commands pinned on a fresh install, so the Favorite tab is not empty the first time it is
 * opened. Only the "on" half of a switch is listed: that is the key the list shows.
 */
object DefaultFavorites {
    val KEYS: List<String> =
        listOf(
            NormalCommand.WifiOn(),
            NormalCommand.DarkThemeOn(),
            NormalCommand.LayoutBorderOn(),
            NormalCommand.PointerLocationOn(),
            NormalCommand.TapEffectOn(),
        ).map { it.favoriteKey }
}
