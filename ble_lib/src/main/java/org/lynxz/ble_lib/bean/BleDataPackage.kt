package org.lynxz.ble_lib.bean

import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import org.lynxz.ble_lib.config.BleConstant
import org.lynxz.ble_lib.config.BlePara
import org.lynxz.ble_lib.crc32
import org.lynxz.ble_lib.util.ByteUtil
import org.lynxz.ble_lib.util.DESUtil
import org.lynxz.ble_lib.util.Logger
import java.io.Serializable
import java.util.*

/**
 * Created by lynxz on 03/06/2017.
 * 接收到的转传数据帧信息
 * 由于蓝牙单次最多传输20字节,因此需要分包发送;
 * 除了信息主体外,额外在头部插入一个head包,用于传输控制信息
 * 在内容传输完毕后,额外多传一个结束包,内容为: [BleConstant.RELAY_DATA_SUCCESS]  [BleConstant.RELAY_DATA_FAIL]
 * */
class BleDataPackage : Serializable {

    companion object {
        private val TAG = "BleDataPackage"
    }

    // head控制信息, 使用一个包来传递
    /**
     * 自定义的蓝牙协议版本号,占据1字节来传输: 0~255
     */
    private var protocolVersion = 0

    /**
     * 加密方式, 占用1字节: 0~255
     * 0-未加密,非0-有加密,并指代具体加密方式
     * 1-DES加密 参考 [org.lynxz.ble_lib.util.DESUtil]
     */
    private var encryptionType = 0

    /**
     * crc校验结果长度...占据1个字节
     */
    private var crcResultLength = 0

    /**
     * 发送端计算发出的crc冗余校验结果(Long结果直接转为byte[]),占用8个字节
     */
    private var crcResult: Long = 0

    /**
     * 发送发出的加密内容字段 [.encryptedContent] 长度,最大值为 Integer.MAX_VALUE,不过一般10k就够了
     * 这里占用4个字节
     */
    private var encryptedContentLength: Int = 0


    // 扩展传输指令,这是我们传输的主体内容,可能经过加密
    private var encryptedContent: String? = null// 加密内容

    // 计算得到一些结果
    private var calcCrcResult: Long = 0// 接收端计算的crc结果,占用5~8个字节(测试了大概10k的数据,得到的范围)
//        get() {
//            if (field == 0L) {
//                field = crc32(encryptedContent)
//            }
//            return field
//        }

    var isValid: Boolean = false// 接收的数据包是否有效,通过crc校验以及最后的结果以及加密串长度
    var isFinished: Boolean = false// 本轮接收数据是否完成
    var plainContent: String? = null// 解密后的内容

    // 附加信息
    private var remoteDevice: BluetoothDevice? = null// 发送端蓝牙设备

    private val DEFAULT_BUFFER_SIZE = 10240//默认缓冲长度:10k字节
    /**
     * 用于缓存每次接收到的数据包(encryptedContent的数据)
     */
    private var mBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
    /**
     * 当前已缓冲的数据(非 head/tail 包部分)长度
     */
    private var mBufferContentSize = 0


    /**
     * 处理本次接收的ble设备发来的数据
     *
     * @param device 发送端的ble设备
     * @param value  本次接收的蓝牙数据
     * @return true-表示本次接收到的是一个有效的head包  false-接收中/接收结束
     */
    fun processReceiveBleData(device: BluetoothDevice?, value: ByteArray?): Boolean {
        if (isFinished || value == null) return false

        // 根据记录的remoteDevice是否为空,来判定是否是新一轮的数据传输
        if (remoteDevice == null) {
            Logger.d("收到的 head 包大小为: ${value.size}", TAG)
            val updateResult = updateHeadInfo(device, value)

            // 若收取的head包解析失败,则结束本轮接收
            if (!updateResult) {
                isFinished = false
                isValid = false
                remoteDevice = null
                mBufferContentSize = 0
            }
            return updateResult
        } else if (remoteDevice?.address.equals(device?.address, ignoreCase = true)) {
            val msg = String(value)
            // 若通过密文内容长度来判断是否传输结束失败(比如丢包了),则通过tail包来判断,若tail包也丢包,则通过超时来判断
            when (msg) {
                BleConstant.RELAY_DATA_SUCCESS -> {
                    isFinished = true
                    checkValidity()
                }

                BleConstant.RELAY_DATA_FAIL -> {
                    isFinished = true
                    isValid = false
                }

                else -> {
                    // 直接通过head包中的内容长度来辨别时候已经接收完毕
                    System.arraycopy(value, 0, mBuffer, mBufferContentSize, value.size)
                    mBufferContentSize += value.size
                    if (mBufferContentSize >= encryptedContentLength) {
                        isFinished = true
                        checkValidity()
                    }
                }
            }
        }
        return false
    }

    /**
     * 生成明文信息,并检查数据有效性
     * 需要在传输完成或者超时时才可进行判断
     */
    fun checkValidity() {
        if (!isFinished) {
            return
        }

        // 对密文进行解密处理
        val encryptedContentBytes = Arrays.copyOf(mBuffer, mBufferContentSize)
        // 判断crc校验结果是否一致
        calcCrcResult = crc32(encryptedContentBytes)

        // 判断消息主体部分(密文)长度是否一致
        if (encryptedContentLength != mBufferContentSize
                || calcCrcResult != crcResult) {
            isValid = false
            Logger.d("ble数据接收完成,但判断不合法 length => $encryptedContentLength v.s. $mBufferContentSize  ,crc => $calcCrcResult v.s. $crcResult", TAG)
            return
        }

        // 判断密文是否可解析成功
        encryptedContent = String(encryptedContentBytes)
        plainContent = encryptedContent
        if (encryptionType == BleConstant.BLE_ENCRYPTION_TYPE_DES) {
            plainContent = DESUtil.deCrypto(encryptedContentBytes, BlePara.desKey)
        }

        if (TextUtils.isEmpty(plainContent)) {
            isValid = false
            Logger.d("接收数据完成, 但密文解析结果为空, isValid =  false", TAG)
            return
        }

        isValid = true
        Logger.d("ble数据接收完成,且合法 $plainContent")
    }

    /**
     * 接收到第一个蓝牙数据包时更新控制信息
     *
     * @param head 接收到的第一个数据包
     */
    fun updateHeadInfo(device: BluetoothDevice?, head: ByteArray?): Boolean {
        // head包至少有12个字节
        if (head == null || head.size < 12) {
            Logger.d("更新ble head包出错: $head")
            return false
        }

        protocolVersion = ByteUtil.toInt(Arrays.copyOfRange(head, 0, 1))
        encryptionType = ByteUtil.toInt(Arrays.copyOfRange(head, 1, 2))
        crcResultLength = ByteUtil.toInt(Arrays.copyOfRange(head, 2, 3))
        // 单包最大20字节
        if (7 + crcResultLength > 20) {
            return false
        }

        remoteDevice = device
        try {
            crcResult = ByteUtil.bytesToLong(Arrays.copyOfRange(head, 3, 3 + crcResultLength))[0]
            val crc1 = ByteUtil.bytesToLong(Arrays.copyOfRange(head, 3, 3 + crcResultLength))[0]
            Logger.d("计算crc $crcResult => $crc1")
            encryptedContentLength = ByteUtil.toInt(Arrays.copyOfRange(head, 3 + crcResultLength, 7 + crcResultLength))
            if (encryptedContentLength >= kotlin.io.DEFAULT_BUFFER_SIZE) {
                //太长的话就直接过滤掉,默认已经达到10k大小了,不然可能会oom
                if (encryptedContentLength >= kotlin.io.DEFAULT_BUFFER_SIZE * 2) {
                    Logger.d("收到ble head包,但数据过长,直接过滤不处理: $encryptedContentLength", TAG)
                    return false
                }
                mBuffer = ByteArray(encryptedContentLength + 20)
            }
        } catch (e: Exception) {
            Logger.d("处理数据包发生异常,请检查数据是否合法: ${e.message}", TAG)
            return false
        }

        Logger.d("updateHeadInfo protocolVersion = $protocolVersion ," +
                "encryptionType = $encryptionType ,crcResultLength = $crcResultLength ," +
                "encryptedContentLength = $encryptedContentLength , crcResult = $crcResult", TAG)
        return true
    }
}