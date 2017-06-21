package org.lynxz.ble_lib.util;

import java.nio.ByteBuffer;

/**
 * Created by lynxz on 05/06/2017.
 * 用于蓝牙转传时,对数据封包head包进行处理
 */
public class ByteUtil {
    /**
     * 将整数转换成字节数组
     *
     * @param iSource   要转换的整数
     * @param iArrayLen 使用的字节数组长度
     */
    public static byte[] toByteArray(int iSource, int iArrayLen) {
        byte[] bLocalArr = new byte[iArrayLen];
        for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
            bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
        }
        return bLocalArr;
    }

    /**
     * 将int值转换为byte数组
     */
    public static byte[] intToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        buffer.putInt(x);
        return buffer.array();
    }

    /**
     * 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
     * 配合 {@link #toByteArray(int, int)} 使用
     */
    public static int toInt(byte[] bRefArr) {
        int iOutcome = 0;
        byte bLoop;

        for (int i = 0; i < bRefArr.length; i++) {
            bLoop = bRefArr[i];
            iOutcome += (bLoop & 0xFF) << (8 * i);
        }
        return iOutcome;
    }

    /**
     * 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
     * 配合 {@link #toByteArray(int, int)} 使用
     */
    public static int toInt(byte bRef) {
        int iOutcome = 0;
        iOutcome += (bRef & 0xFF);
        return iOutcome;
    }

    /**
     * 将long值转换为byte数组
     */
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long[] bytesToLong(byte[] buf) {
        byte bLength = 8;
        long[] s = new long[buf.length / bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }

            s[iLoop] = getLong(temp, true);
        }

        return s;
    }

    public static long getLong(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        }

        long r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00000000000000ff);
            }
        }
        return r;
    }

}