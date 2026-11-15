package jp.kaleidot725.adbpad.domain.model.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToggleCommandDefTest {
    private fun parser(onCommand: NormalCommand) = ToggleCommandDef.ON_KEY_MAP.getValue(onCommand.favoriteKey).parseIsOn

    @Test
    fun `data saver reads the enabled or disabled wording real devices print`() {
        val isOn = parser(NormalCommand.DataSaverOn())
        assertEquals(true, isOn("Restrict background status: enabled"))
        assertEquals(false, isOn("Restrict background status: disabled"))
    }

    @Test
    fun `wifi counts the airplane override value as on`() {
        val isOn = parser(NormalCommand.WifiOn())
        assertEquals(true, isOn("1"))
        assertEquals(true, isOn("2\n"))
        assertEquals(false, isOn("0"))
        assertEquals(false, isOn("null"))
    }

    @Test
    fun `location is on only for a positive mode`() {
        val isOn = parser(NormalCommand.LocationOn())
        assertEquals(true, isOn("3"))
        assertEquals(false, isOn("0"))
        assertEquals(false, isOn("null"))
        assertEquals(false, isOn(""))
        assertEquals(false, isOn("Error: something went wrong"))
    }
}
