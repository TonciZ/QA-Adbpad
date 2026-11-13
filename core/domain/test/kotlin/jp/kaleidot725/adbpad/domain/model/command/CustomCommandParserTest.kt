package jp.kaleidot725.adbpad.domain.model.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomCommandParserTest {
    @Test
    fun `pasted two line script becomes two adb argument lists`() {
        val input = "adb connect 192.168.1.20:5555\nadb shell \"grep MemTotal /proc/meminfo\"\n"

        assertEquals(
            listOf(
                listOf("connect", "192.168.1.20:5555"),
                listOf("shell", "grep MemTotal /proc/meminfo"),
            ),
            CustomCommandParser.parse(input),
        )
    }

    @Test
    fun `leading adb is optional and blank lines are skipped`() {
        assertEquals(
            listOf(listOf("devices", "-l"), listOf("shell", "getprop")),
            CustomCommandParser.parse("  devices -l\r\n\r\n   \nADB.exe shell getprop"),
        )
    }

    @Test
    fun `quotes group words and backslashes stay literal`() {
        assertEquals(
            listOf("push", "C:\\Users\\me\\My App.apk", "/sdcard/"),
            CustomCommandParser.tokenize("push \"C:\\Users\\me\\My App.apk\" /sdcard/"),
        )
        assertEquals(listOf("shell", "echo it's"), CustomCommandParser.tokenize("shell \"echo it's\""))
        assertEquals(listOf("shell", "echo \"hi\""), CustomCommandParser.tokenize("shell \"echo \\\"hi\\\"\""))
        assertEquals(listOf("shell", "input text a b"), CustomCommandParser.tokenize("shell 'input text a b'"))
        assertEquals(listOf("shell", ""), CustomCommandParser.tokenize("shell \"\""))
    }

    @Test
    fun `selected device is targeted for device commands only`() {
        assertEquals(
            listOf("-s", "emulator-5554", "shell", "getprop"),
            CustomCommandParser.withDevice(listOf("shell", "getprop"), "emulator-5554"),
        )
        assertEquals(listOf("connect", "10.0.0.2"), CustomCommandParser.withDevice(listOf("connect", "10.0.0.2"), "emulator-5554"))
        assertEquals(listOf("devices"), CustomCommandParser.withDevice(listOf("devices"), "emulator-5554"))
        assertEquals(listOf("-d", "shell", "ls"), CustomCommandParser.withDevice(listOf("-d", "shell", "ls"), "emulator-5554"))
        assertEquals(listOf("shell", "ls"), CustomCommandParser.withDevice(listOf("shell", "ls"), null))
    }

    @Test
    fun `connect target gets the default port when none is given`() {
        assertEquals("192.168.1.20:5555", CustomCommandParser.connectTarget(listOf("connect", "192.168.1.20")))
        assertEquals("192.168.1.20:4321", CustomCommandParser.connectTarget(listOf("connect", "192.168.1.20:4321")))
        assertNull(CustomCommandParser.connectTarget(listOf("shell", "connect", "x")))
    }

    @Test
    fun `display re-quotes arguments that contain spaces`() {
        assertEquals(
            "adb -s 192.168.1.20:5555 shell \"grep MemTotal /proc/meminfo\"",
            CustomCommandParser.display(listOf("-s", "192.168.1.20:5555", "shell", "grep MemTotal /proc/meminfo")),
        )
    }
}
