package org.lynxz.ble_lib.callbacks

import android.bluetooth.*
import android.os.CountDownTimer
import android.os.Looper
import org.lynxz.ble_lib.bean.BleDataPackage
import org.lynxz.ble_lib.config.BleConstant
import org.lynxz.ble_lib.config.BlePara
import org.lynxz.ble_lib.util.Logger
import java.util.*

/**
 * Created by lynxz on 19/06/2017.
 * 接收数据时,通过本类回调处理
 */
class GattServerCallBack : BluetoothGattServerCallback() {

    companion object {
        private val TAG = "GattServerCallBack"
    }

    var onRelayListener: OnRelayListener? = null
    private var mGattServer: BluetoothGattServer? = null
    private var mOnRelayListener: OnRelayListener? = null // 回调接口,用于收到信息后发送给app层

    /**
     * 当前与本机交互的蓝牙设备相关信息及数据包
     */
    private var mCurrentBlePackage = BleDataPackage()

    /**
     * 单次接收超时限制,目前暂定为10s
     * 超时后,丢弃本次接收,并开始新的接收
     * */
    private val mCountdownTimer: CountDownTimer by lazy {
        Looper.prepare()
        object : CountDownTimer(BleConstant.DEFAULT_RELAY_TIME_OUT, BleConstant.DEFAULT_RELAY_TIME_OUT) {
            override fun onFinish() {
            }

            override fun onTick(millisUntilFinished: Long) {
                Logger.d("receive ble data timeout...")
                mCurrentBlePackage.isFinished = true
                mCurrentBlePackage = BleDataPackage()
            }
        }
    }

    /**
     * 初始化需要用来转传数据的 service/characteristic
     * */
    private val mRelayService by lazy {
        val service = BluetoothGattService(UUID.fromString(BleConstant.RELAY_SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val characteristic = BluetoothGattCharacteristic(
                UUID.fromString(BleConstant.RELAY_CHARACTERISTIC_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ
                        or BluetoothGattCharacteristic.PROPERTY_WRITE
                        or BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, // 这里设定不需要回应,也可选择需要响应模式
                BluetoothGattCharacteristic.PERMISSION_READ
                        or BluetoothGattCharacteristic.PERMISSION_WRITE)// 可写模式,不同ble设备间通过本characteristic来传输数据
        characteristic.setValue(BlePara.adCharacteristicValue)

        val addCharacteristic = service.addCharacteristic(characteristic)
        Logger.d("addCharacteristic result = $addCharacteristic", TAG)
        service
    }

    /**
     * 广播开始后,设置一个用于接收消息的service
     * 后续有数据传入时,会触发 [org.lynxz.ble_lib.callbacks.GattServerCallBack.onCharacteristicWriteRequest]
     * */
    fun setupServices(gattServer: BluetoothGattServer?): Boolean {
        if (gattServer == null) {
            return false
        }

        // 设置一个GattService以及BluetoothGattCharacteristic
        mGattServer = gattServer
        val service = mGattServer?.getService(UUID.fromString(BleConstant.RELAY_SERVICE_UUID))
        if (service == null) {
            val addResult = mGattServer?.addService(mRelayService)
            Logger.d("  -> 添加自定义service...result = $addResult", TAG)
        } else {
            Logger.d("  -> 添加自定义service... service已存在,不用重复添加", TAG)
        }
        return true
    }

    /**
     * ble设备连接状态发生变化时回调
     * */
    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        Logger.d("${device?.name} ${device?.address}   $status -> $newState")
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
        // 按需发送响应
        var responseResult = true
        if (responseNeeded) responseResult = mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null) ?: false
        Logger.d("responseNeeded = $responseNeeded ,send response result = $responseResult , receive data length = ${value?.size}")

        // 处理收到的数据
        // 判断是否是 head 控制头信息,若是,则表明这是一个新数据包
        val isNewData = mCurrentBlePackage.processReceiveBleData(device, value)
        if (isNewData) {// 开始接收一个新数据, 开启超时判定
            mCountdownTimer.start()
        }

        // 已完成接收的情况
        if (mCurrentBlePackage.isFinished) {
            mCountdownTimer.cancel()
            val valid = mCurrentBlePackage.isValid
            Logger.d("完成接收ble数据,解析结果 valid = $valid  发送方是: ${device?.address} ${device?.name}")
            if (valid) {
                mOnRelayListener?.onReceive(mCurrentBlePackage.plainContent)
            }
            mCurrentBlePackage = BleDataPackage()
        }
    }

    fun release() {
        mCountdownTimer.cancel()
        mCurrentBlePackage = BleDataPackage()
    }
}