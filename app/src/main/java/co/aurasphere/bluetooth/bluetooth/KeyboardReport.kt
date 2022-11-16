package co.aurasphere.bluetooth.bluetooth

import java.lang.StringBuilder

object KeyboardReport {
    val keyboardData = ByteArray(2)
    fun getReport(modifier: Int, key: Int): ByteArray {
        //No need to fill with zero now
//        Arrays.fill(keyboardData, (byte) 0);
        keyboardData[0] = modifier.toByte()
        //        keyboardData[1] = 0;    //reserve byte
        keyboardData[1] = key.toByte()
        //        keyboardData[3] = 0;    //End reserve byte
        return keyboardData
    }

    fun print(bytes: ByteArray): String {
        val sb = StringBuilder()
        sb.append("[ ")
        for (b in bytes) {
            sb.append(String.format("0x%02X, ", b))
        }
        sb.append("]")
        return sb.toString()
    }
}
