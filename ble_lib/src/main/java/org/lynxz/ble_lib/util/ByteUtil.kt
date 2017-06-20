package org.lynxz.ble_lib.util


import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * Created by lynxz on 05/06/2017.
 * 用于蓝牙转传时,对数据封包head包进行处理
 */
object ByteUtil {
    /**
     * 将整数转换成字节数组
     * @param iSource   要转换的整数
     * @param iArrayLen 使用的字节数组长度
     */
    fun toByteArray(iSource: Int, iArrayLen: Int): ByteArray {
        val bLocalArr = ByteArray(iArrayLen)
        var i = 0
        while (i < 4 && i < iArrayLen) {
            bLocalArr[i] = (iSource shr 8 * i and 0xFF).toByte()
            i++
        }
        return bLocalArr
    }

    /**
     * 将int值转换为byte数组
     */
    fun intToBytes(x: Int): ByteArray {
        val buffer = ByteBuffer.allocate(Integer.SIZE / java.lang.Byte.SIZE)
        buffer.putInt(x)
        return buffer.array()
    }

    /**
     * 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
     * 配合 [.toByteArray] 使用
     */
    fun toInt(bRefArr: ByteArray): Int {
        var iOutcome = 0
        var bLoop: Byte

        for (i in bRefArr.indices) {
            bLoop = bRefArr[i]
            iOutcome += (bLoop and 0xFF.toByte()).toInt().shl(8 * i)
        }
        return iOutcome
    }

    /**
     * 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
     * 配合 [.toByteArray] 使用
     */
    fun toInt(bRef: Byte): Int {
        var iOutcome = 0
        iOutcome += bRef and 0xFF.toByte()
        return iOutcome
    }

    /**
     * 将long值转换为byte数组
     */
    fun longToBytes(x: Long): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Long.SIZE / java.lang.Byte.SIZE)
        buffer.putLong(x)
        return buffer.array()
    }

    fun bytesToLong(buf: ByteArray): LongArray {
        val bLength = 8
        val s = LongArray(buf.size / bLength)

        for (iLoop in s.indices) {
            val temp = ByteArray(bLength)

            for (jLoop in 0..bLength - 1) {
                temp[jLoop] = buf[iLoop * bLength + jLoop]
            }

            s[iLoop] = getLong(temp, true)
        }

        return s
    }

    fun getLong(buf: ByteArray?, bBigEnding: Boolean): Long {
        if (buf == null) {
            throw IllegalArgumentException("byte array is null!")
        }

        if (buf.size > 8) {
            throw IllegalArgumentException("byte array size > 8 !")
        }

        var r: Long = 0
        if (bBigEnding) {
            for (i in buf.indices) {
                r = r shl 8
                r = r or (buf[i] and 0x00000000000000ff.toByte()).toLong()
            }
        } else {
            for (i in buf.indices.reversed()) {
                r = r shl 8
                r = r or (buf[i] and 0x00000000000000ff.toByte()).toLong()
            }
        }
        return r
    }
}