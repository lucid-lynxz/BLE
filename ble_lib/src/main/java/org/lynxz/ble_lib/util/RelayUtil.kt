package org.lynxz.ble_lib.util

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.lynxz.ble_lib.config.BleConstant
import org.lynxz.ble_lib.config.BlePara
import org.lynxz.ble_lib.crc32
import java.util.*

object RelayUtil {
    val TAG = "RelayUtil"

    fun relayData(gatt: BluetoothGatt, msg: String) = runBlocking {
        val send = async(CommonPool) {
            sendDataByCharacteristic(gatt, msg)
        }
        val await = send.await()
        await
    }

    suspend fun sendDataByCharacteristic(gatt: BluetoothGatt, msg: String): Boolean {
        val startTime = System.currentTimeMillis()
        if (msg.isEmpty()) {
            return false
        }
        val service = gatt.getService(UUID.fromString(BleConstant.RELAY_SERVICE_UUID)) ?: return false
        val relayChar = service.getCharacteristic(UUID.fromString(BleConstant.RELAY_CHARACTERISTIC_UUID)) ?: return false

        var result = true// 发送数据是否成功
        // 发送head包
        val headPackage = ByteArray(20)
        headPackage[0] = ByteUtil.toByteArray(BleConstant.BLE_PROTOCOL_VERSION, 1)[0]//协议版本,1字节
        headPackage[1] = ByteUtil.toByteArray(BleConstant.BLE_ENCRYPTION_TYPE_DES, 1)[0]//加密类型,1字节

        // 密文crc校验码
        Logger.d("要发送的数据是: $msg")
        val encryptedContentBytes = DESUtil.enCrypto(msg.toByteArray(), BlePara.desKey)// 加密后的字符串
        if (encryptedContentBytes == null) {
            Logger.d("加密失败,不进行ble转传")
            return false
        }

        val crcResult = crc32(encryptedContentBytes)
        val crcResultBytes = ByteUtil.longToBytes(crcResult)
        headPackage[2] = ByteUtil.toByteArray(crcResultBytes.size, 1)[0]// crc校验结果长度,其实不用设置,固定8字节
        Logger.d("发送时,校验密文crc结果: $crcResult  ${crcResultBytes.size}")

        System.arraycopy(crcResultBytes, 0, headPackage, 3, crcResultBytes.size)

        // 加密串长度
        val encryptedContentLength = ByteUtil.toByteArray(encryptedContentBytes.size, 4)//加密串长度值转byte用于传输,占用4字节
        System.arraycopy(encryptedContentLength, 0, headPackage, 3 + crcResultBytes.size, 4)

        relayChar.value = headPackage
        relayChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        result = gatt.writeCharacteristic(relayChar)

        // 发送密文数据主体
        val size = encryptedContentBytes.size
        Logger.d("$result head 长度 = ${headPackage.size} ,ble传输密文长度: $size, 对应的明文长度: ${msg.toByteArray().size}")
        if (!result) {
            Logger.d("head包传输失败,密文内容不需要再传")
            return false
        }

        val delay = 20
        try {
            // 注意,这里需要延时一下,不然测试发现,基本上只能收到其中几帧的数据,失败的概率比较大
            Thread.sleep(delay.toLong())

            var i = 0
            while (i < size) {
                var to = i + 20
                if (to >= size) {
                    to = size
                }
                val slice = Arrays.copyOfRange(encryptedContentBytes, i, to)
                relayChar.value = slice
                var sliceResult = gatt.writeCharacteristic(relayChar)
                Logger.d("传送第 $i ~ $to 块数据的结果: $sliceResult", TAG)
                // 领导确定,发送失败时,直接忽略,无需重传,我这里还是尝试重传一次就好
                if (!sliceResult) {
                    Thread.sleep(200)
                    sliceResult = gatt.writeCharacteristic(relayChar)
                    Logger.d(" =>重传第 $i ~ $to 块数据的结果: $sliceResult", TAG)
                }
                result = result and sliceResult
                i = to
                Thread.sleep(delay.toLong())
                // 由于只重传一次, 因此如果某个数据分包重传失败,则不必要再传后续数据,直接返回失败
                if (!result) {
                    break
                }
            }

            // 传输结束标志位:自定义
            val end = if (result) BleConstant.RELAY_DATA_SUCCESS else BleConstant.RELAY_DATA_FAIL
            relayChar.value = end.toByteArray()
            val tailResult = gatt.writeCharacteristic(relayChar)
            Logger.d("发送tail包结果: $tailResult")
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }
        val timeConsume = System.currentTimeMillis() - startTime
        Logger.d("发送ble数据最终结果: $result 总耗时: $timeConsume", TAG)
        return result
    }
}

