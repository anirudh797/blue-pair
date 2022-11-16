package co.aurasphere.bluetooth.bluetooth

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object KeyboardHelper {
    val keyMap: Map<Char, Int> = mapOf(
        Pair('a', 0x04),
        Pair('b', 0x05),
        Pair('e', 0x08),
        Pair('d', 0x07),
        Pair('c', 0x06),
        Pair('f', 0x09),
        Pair('g', 0x0A),
        Pair('h', 0x0B),
        Pair('i', 0x0C),
        Pair('j', 0x0D),
        Pair('k', 0x0E),
        Pair('m', 0x10),
        Pair('n', 0x11),
        Pair('l', 0x0F),
        Pair('o', 0x12),
        Pair('p', 0x13),
        Pair('q', 0x14),
        Pair('r', 0x15),
        Pair('s', 0x16),
        Pair('t', 0x17),
        Pair('u', 0x18),
        Pair('v', 0x19),
        Pair('w', 0x1A),
        Pair('x', 0x1B),
        Pair('y', 0x1C),
        Pair('z', 0x1D),
        Pair('1', 0x1E),
        Pair('2', 0x1F),
        Pair('3', 0x20),
        Pair('4', 0x21),
        Pair('5', 0x22),
        Pair('6', 0x23),
        Pair('7', 0x24),
        Pair('8', 0x25),
        Pair('9', 0x26),
        Pair('0', 0x27),
        Pair(' ', 0x2C),
        Pair('-', 0x2D),
        Pair('=', 0x2E),
        Pair('[', 0x2F),
        Pair(']', 0x30),
        Pair('\\', 0x31),
        Pair(';', 0x33),
        Pair('\'', 0x34),
        Pair('`', 0x35),
        Pair(',', 0x36),
        Pair('.', 0x37),
        Pair('/', 0x38)
    )
    val shiftKeyMap: Map<Char, Int> = mapOf(
        Pair('A', 0x04),
        Pair('B', 0x05),
        Pair('C', 0x06),
        Pair('D', 0x07),
        Pair('E', 0x08),
        Pair('F', 0x09),
        Pair('G', 0x0A),
        Pair('H', 0x0B),
        Pair('I', 0x0C),
        Pair('J', 0x0D),
        Pair('K', 0x0E),
        Pair('L', 0x0F),
        Pair('M', 0x10),
        Pair('N', 0x11),
        Pair('O', 0x12),
        Pair('P', 0x13),
        Pair('Q', 0x14),
        Pair('R', 0x15),
        Pair('S', 0x16),
        Pair('T', 0x17),
        Pair('U', 0x18),
        Pair('V', 0x19),
        Pair('W', 0x1A),
        Pair('X', 0x1B),
        Pair('Y', 0x1C),
        Pair('Z', 0x1D),
        Pair('!', 0x1E),
        Pair('@', 0x1F),
        Pair('#', 0x20),
        Pair('$', 0x21),
        Pair('%', 0x22),
        Pair('^', 0x23),
        Pair('&', 0x24),
        Pair('*', 0x25),
        Pair('(', 0x26),
        Pair(')', 0x27),
        Pair('_', 0x2D),
        Pair('+', 0x2E),
        Pair('{', 0x2F),
        Pair('}', 0x30),
        Pair('|', 0x31),
        Pair(':', 0x33),
        Pair('"', 0x34),
        Pair('~', 0x35),
        Pair('<', 0x36),
        Pair('>', 0x37),
        Pair('?', 0x38)
    )

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendKeyDown(@Modifier modifier: Int, code: Int,bluetoothController: BluetoothController): Boolean? {
        return if (bluetoothController.bluetoothHidDevice != null && bluetoothController.isHidDeviceConnected ) {
            bluetoothController?.bluetoothHidDevice?.sendReport(
                bluetoothController.mmDevice,
                Constants.ID_KEYBOARD.toInt(),
                KeyboardReport.getReport(modifier, code)
            )
        }
        else false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendKeyUp(bluetoothController: BluetoothController): Boolean? {
        return if (bluetoothController.bluetoothHidDevice != null && bluetoothController.isHidDeviceConnected) {
            bluetoothController.bluetoothHidDevice?.sendReport(
                bluetoothController.mmDevice,
                Constants.ID_KEYBOARD.toInt(),
                KeyboardReport.getReport(0, 0)
            )
        } else false
    }

    fun getKey(c: Char): Int {
        return keyMap[c] ?: 0
    }

    fun getShiftKey(c: Char): Int {
        return shiftKeyMap[c] ?: 0
    }

    annotation class Modifier {
        companion object {
            var NONE = 0
            var KEY_MOD_LCTRL = 0x01
            var KEY_MOD_LSHIFT = 0x02
            var KEY_MOD_LALT = 0x04
            var KEY_MOD_LMETA = 0x08
            var KEY_MOD_RCTRL = 0x10
            var KEY_MOD_RSHIFT = 0x20
            var KEY_MOD_RALT = 0x40
            var KEY_MOD_RMETA = 0x80
            var N = 0x00
        }
    }

    @Retention(RetentionPolicy.SOURCE)
//    @IntDef(
//        Key.ENTER,
//        Key.ESCAPE,
//        Key.BACKSPACE,
//        Key.TAB,
//        Key.SPACE,
//        Key.RIGHT,
//        Key.LEFT,
//        Key.DOWN,
//        Key.UP
//    )
    annotation class Key {
        companion object {
            var ENTER = 0x28
            var ESCAPE = 0x29
            var BACKSPACE = 0x2a
            var TAB = 43
            var SPACE = 0x2c
            var RIGHT = 79
            var LEFT = 80
            var DOWN = 81
            var UP = 82
            var KEY_KP1 = 0x59 // Keypad 1 and End
            var KEY_KP2 = 0x5a // Keypad 2 and Down Arrow
            var KEY_KP3 = 0x5b // Keypad 3 and PageDn
            var KEY_KP4 = 0x5c // Keypad 4 and Left Arrow
            var KEY_KP5 = 0x5d // Keypad 5
            var KEY_KP6 = 0x5e // Keypad 6 and Right Arrow
            var KEY_KP7 = 0x5f // Keypad 7 and Home
            var KEY_KP8 = 0x60 // Keypad 8 and Up Arrow
            var KEY_KP9 = 0x61 // Keypad 9 and Page Up
            var KEY_KP0 = 0x62 // Keypad 0 and Insert
            var KEY_KPDOT = 0x63 // Keypad . and Delete
            var KEY_PROPS = 0x76 // Keyboard Menu
            var KEY_POWER = 0x66
            var KEY_APPLICATION = 0x65 // Keyboard Application
            var KEY_VOLUMEUP = 0x80 // Keyboard Volume Up
            var KEY_VOLUMEDOWN = 0x81 // Keyboard Volume Down
            var KEY_HOME = 0x4a
            var KEY_MEDIA_PLAYPAUSE = 0xe8
            var KEY_MEDIA_STOPCD = 0xe9
            var KEY_MEDIA_PREVIOUSSONG = 0xea
            var KEY_MEDIA_NEXTSONG = 0xeb
            var KEY_MEDIA_EJECTCD = 0xec
            var KEY_MEDIA_VOLUMEUP = 0xed
            var KEY_MEDIA_VOLUMEDOWN = 0xee
            var KEY_MEDIA_MUTE = 0xef
            var KEY_MEDIA_WWW = 0xf0
            var KEY_MEDIA_BACK = 0xf1
            var KEY_MEDIA_FORWARD = 0xf2
            var KEY_MEDIA_STOP = 0xf3
            var KEY_MEDIA_FIND = 0xf4
            var KEY_MEDIA_SCROLLUP = 0xf5
            var KEY_MEDIA_SCROLLDOWN = 0xf6
            var KEY_MEDIA_EDIT = 0xf7
            var KEY_MEDIA_SLEEP = 0xf8
            var KEY_MEDIA_COFFEE = 0xf9
            var KEY_MEDIA_REFRESH = 0xfa
            var KEY_MEDIA_CALC = 0xfb
        }
    }
}