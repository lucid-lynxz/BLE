package org.lynxz.ble_lib

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.widget.Toast
import org.lynxz.ble_lib.util.Logger
import java.util.zip.CRC32

/**
 * Created by lynxz on 19/06/2017.
 */

/**
 * 验证本机是否支持低功耗蓝牙(BLE)
 * 只考虑5.0以上设备,5.0y以上才支持 peripheral 模式
 * 4.3以上虽然文档说支持BLE,但是我测试时基本搜索不到BLE从机,因此不考虑
 */
fun Context.isSupportBle(): Boolean {
    val packageManager = packageManager
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}

fun Any.getDefaultAdapter(context: Context): BluetoothAdapter? {
    var adapter: BluetoothAdapter? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager.adapter
    }
    return adapter
}

/**
 * 手机蓝牙是否开启
 */
fun Any.isBluetoothOn(cxt: Context): Boolean {
    val defaultAdapter = getDefaultAdapter(cxt)
    return defaultAdapter != null && defaultAdapter.isEnabled
}

/**
 * 将字符串转换位long,若转换失败则返回0
 */
fun String?.convertLongValue(): Long {
    var result = 0L
    try {
        result = this?.toLong() ?: 0L
    } catch (e: Exception) {
        Logger.d("error occur when convert string to long  ${e.message}")
    }
    return result
}

/**
 * 将字符串转换位int,若转换失败则返回0
 */
fun String?.convertIntValue(): Int {
    var value = 0
    try {
        value = this?.toInt() ?: 0
    } catch (e: Exception) {
        e.printStackTrace()
        Logger.d("error occur when convert string to int  ${e.message}")
    }

    return value
}

/**
 * 对给定的字符串进行crc32校验,8字节
 * 若是字符串为空,则返回null
 */
fun Any.crc32(content: String?): Long {
    if (TextUtils.isEmpty(content)) {
        return 0
    }
    val c32 = CRC32()
    c32.update(content!!.toByteArray())
    return c32.value
}

fun Any.crc32(content: ByteArray?): Long {
    if (content == null || content.isEmpty()) {
        return 0
    }
    val c32 = CRC32()
    c32.update(content)
    return c32.value
}

fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showToast(msgId: Int) {
    Toast.makeText(this, msgId, Toast.LENGTH_SHORT).show()
}