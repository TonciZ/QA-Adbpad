package jp.kaleidot725.adbpad.domain.model.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultFavoritesTest {
    @Test
    fun `default pins are distinct and not blank`() {
        assertTrue(DefaultFavorites.KEYS.isNotEmpty())
        assertTrue(DefaultFavorites.KEYS.none { it.isBlank() })
        assertEquals(DefaultFavorites.KEYS.size, DefaultFavorites.KEYS.toSet().size)
    }

    @Test
    fun `default pins are never the hidden off half of a switch`() {
        // The command list drops "off" keys, so a default pinned by its off key would not show.
        assertTrue(DefaultFavorites.KEYS.none { it in ToggleCommandDef.OFF_KEYS })
    }
}
