package org.lynxz.ble_lib

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import org.lynxz.ble_lib.callbacks.GattServerCallBack
import org.lynxz.ble_lib.callbacks.OnRelayListener
import org.lynxz.ble_lib.util.Logger

/**
 * Created by lynxz on 19/06/2017.
 * 蓝牙相关收发操作都放置于service中进行处理
 */
class BleService : Service() {
    val mBinder: BleBinder? = null

    // 用于存储符合条件的蓝牙设备,已广播过滤码为准进行过滤操作
    val mDevices: MutableList<BluetoothDevice> = mutableListOf()
    // 存储已经缓存过的蓝牙设备地址,用于扫描时不需要重新判断
    val mBleAddressSet: MutableSet<String> = mutableSetOf()

    private var isAdvertising = false // 是否正在广播
    private var isScanningBle = false // 是否正在扫描ble设备
    private var isSupportAdvertisement = false// 是否支持peripheral模式

    private var mOnRelayListener: OnRelayListener? = null

    var mBluetoothManager: BluetoothManager? = null
    var mBluetoothAdapter: BluetoothAdapter? = null

    private var mLeScanCallback: ScanCallback? = null // ble 扫描回调
    private var mAdCallback: AdvertiseCallback? = null// 广播回调

    private var mGattServer: BluetoothGattServer? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null// central模式主机扫描
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null // 支持 peripheral 模式,可被扫描到

    val mGattServerCallBack: GattServerCallBack by lazy {
        val gatt = GattServerCallBack()
        gatt.onRelayListener = mBinder?.onRelayListener
        gatt
    }

    override fun onBind(intent: Intent?) = mBinder

    override fun onCreate() {
        super.onCreate()
        Logger.d("ble service onCreated...")

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            /**
             * 初始化ble设定
             * */
    fun initBleSettings() {
        if (!isSupportBle()) {
            return
        }

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        // null indicated that this device is not support bluetooth
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager?.adapter
            mBluetoothLeScanner = mBluetoothAdapter?.bluetoothLeScanner
            mBluetoothLeAdvertiser = mBluetoothAdapter?.bluetoothLeAdvertiser
            isSupportAdvertisement = mBluetoothAdapter?.isMultipleAdvertisementSupported ?: false
            mBluetoothManager?.openGattServer(this, mGattServerCallBack)
        }

    }

    class BleBinder : Binder() {
        fun updateGatt(){

        }
        var onRelayListener: OnRelayListener? = null
            set(value) {

            }

    }
}