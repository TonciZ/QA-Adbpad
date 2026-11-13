package jp.kaleidot725.adbpad.domain.model.setting

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WirelessAdbTargetTest {
    @Test
    fun `plain ip uses the given port`() {
        assertEquals(WirelessAdbTarget("192.168.1.5", 5555), WirelessAdbTarget.parse(" 192.168.1.5 ", 5555))
    }

    @Test
    fun `port in the text wins`() {
        assertEquals(WirelessAdbTarget("192.168.1.5", 4321), WirelessAdbTarget.parse("192.168.1.5:4321", 5555))
    }

    @Test
    fun `pasted adb connect line is understood`() {
        assertEquals(WirelessAdbTarget("10.0.0.2", 5555), WirelessAdbTarget.parse("adb connect 10.0.0.2:5555", 1))
    }
}
