package org.lynxz.ble_lib

import android.annotation.TargetApi
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.*
import org.lynxz.ble_lib.callbacks.GattServerCallBack
import org.lynxz.ble_lib.callbacks.OnRelayListener
import org.lynxz.ble_lib.config.BleConstant
import org.lynxz.ble_lib.config.BlePara
import org.lynxz.ble_lib.util.Logger
import org.lynxz.ble_lib.util.RelayUtil

/**
 * Created by lynxz on 19/06/2017.
 * 蓝牙相关收发操作都放置于service中进行处理
 */
class BleService : Service() {

    companion object {
        val TAG = "BleService"
    }

    val mBinder: BleBinder  by lazy { BleBinder() }

    // 用于存储符合条件的蓝牙设备,已广播过滤码为准进行过滤操作
    val mDevices: MutableList<BluetoothDevice> = mutableListOf()
    // 存储已经缓存过的蓝牙设备地址,用于扫描时不需要重新判断
    val mBleAddressSet: MutableSet<String> = mutableSetOf()

    private var isAdvertising = false // 是否正在广播
    private var isScanningBle = false // 是否正在扫描ble设备
    private var isSupportAdvertisement = false// 是否支持peripheral模式

    var mBluetoothManager: BluetoothManager? = null
    var mBluetoothAdapter: BluetoothAdapter? = null

    private var mLeScanCallback: ScanCallback? = null // ble 扫描回调
    private var mAdCallback: AdvertiseCallback? = null// 广播回调

    private var mGattServer: BluetoothGattServer? = null
    private var mBluetoothLeScanner: BluetoothLeScanner? = null// central模式主机扫描
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null // 支持 peripheral 模式,可被扫描到

    private var mGattCallback: BluetoothGattCallback? = null
    private var mGatt: BluetoothGatt? = null//当前连接的ble设备管理器

    private var mOnRelayListener: OnRelayListener? = null

    val mGattServerCallBack: GattServerCallBack by lazy {
        val gatt = GattServerCallBack()
        gatt.onRelayListener = mBinder.onRelayListener
        gatt
    }

    /**
     * 发送数据超时器
     */
    private val mCountDownTimer = object : CountDownTimer(BleConstant.DEFAULT_RELAY_TIME_OUT, BleConstant.DEFAULT_RELAY_TIME_OUT) {
        override fun onTick(millisUntilFinished: Long) {

        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onFinish() {
            Logger.d("mCountDownTimer 已超时", TAG)
            // todo 超时进行下一台设备的传送
            skipToNextDevice()
        }
    }

    val AUTO_STOP_SCAN_DELAY = 10 * 1000L//启动扫描后10秒,自动停止扫描
    val MSG_TYPE_AUTO_START_SCAN = 1//自动开始扫描ble设备
    val MSG_TYPE_AUTO_STOP_SCAN = 2//自动停止扫描
    val MSG_TYPE_START_SEND_DATA = 3//开始对扫描到的ble设备列表进行数据转传
    val MSG_TYPE_SEND_DATA_TO_THE_GATT = 4//ble连接成功并扫描到service后,开始进行数据发送
    val MSG_TYPE_SEND_DATA_TO_NEXT = 5//请进行下一个ble设备的数据转传

    var mCurrentRelayMsg: String = ""
    var currentRelayBleIndex = -1//当前进行转传的ble设备在mDevices列表中的序号,-1表示未开始

    val mHandler = object : Handler(Looper.getMainLooper()) {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val size = mDevices.size
            when (msg?.what) {
                MSG_TYPE_AUTO_START_SCAN -> mBinder.startScanLeDevices()
                MSG_TYPE_AUTO_STOP_SCAN -> mBinder.stopScanLeDevices()
                MSG_TYPE_START_SEND_DATA -> if (size > 0) {
                    disconnectLastGatt()
                    currentRelayBleIndex = 0
                    val targetDevice = mDevices[currentRelayBleIndex]
                    mCountDownTimer.start()
                    mGatt = targetDevice.connectGatt(this@BleService, false, mGattCallback)
                } else {
                    currentRelayBleIndex = -1
                }

                MSG_TYPE_SEND_DATA_TO_THE_GATT -> {
                    val gatt = msg.obj as BluetoothGatt
                    val relayData = RelayUtil.relayData(gatt, mCurrentRelayMsg)
                    Logger.d("发送数据给设备是否成功: " + relayData)
//                    Thread(Runnable { sendDataByCharacteristic(gatt, mCurrentBdData) }).start()
                    sendEmptyMessage(MSG_TYPE_SEND_DATA_TO_NEXT)
                }

                MSG_TYPE_SEND_DATA_TO_NEXT -> if (size > 0) {
                    mCountDownTimer.cancel()
                    disconnectLastGatt()
                    currentRelayBleIndex += 1
                    if (currentRelayBleIndex >= 0 && currentRelayBleIndex < size) {
                        val device = mDevices[currentRelayBleIndex]
                        // 这里其实可以多加一个判断转传信息来源与要发送的目标设备不是同一台设备,避免信息重复传输,不过貌似蓝牙mac地址动态变化,也不一定有效,在此省略
                        Logger.d("正准备进行下一台设备的转传: $currentRelayBleIndex/$size", TAG)
                        mCountDownTimer.start()
                        mGatt = device.connectGatt(this@BleService, false, mGattCallback)
                    } else {
                        currentRelayBleIndex = -1
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate() {
        super.onCreate()
        initBleSettings()
        Logger.d("ble service onCreated...")
    }

    /**
     * 关闭上一次的连接
     * */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun disconnectLastGatt() {
        mGatt?.disconnect()
        //            mGatt?.close();
        //            mGatt = null;
    }

    /**
     * 初始化ble设定
     * */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
            mGattServer = mBluetoothManager?.openGattServer(this, mGattServerCallBack)
            Logger.d("mGattServer created .... " + mGattServer)
        }

        // 低功耗蓝牙扫描回调
        mLeScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
//                Logger.d("scan successful $result")
                addBleDevice(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                results?.forEach { addBleDevice(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                if (ScanCallback.SCAN_FAILED_ALREADY_STARTED != errorCode) {
                    isScanningBle = false
                }
                Logger.d("scan failed errorCode = $errorCode")
            }
        }

        // 广播回调
        mAdCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                isAdvertising = true
                if (settingsInEffect != null) {
                    Logger.d("TxPowerLv = ${settingsInEffect.txPowerLevel}\nmode = ${settingsInEffect.mode}\ntimeout = ${settingsInEffect.timeout}")
                } else {
                    Logger.d("settingInEffect is null")
                }
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                isAdvertising = false
                Logger.d("errorCode = $errorCode")
            }
        }

        mGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                val device = gatt.device
                Logger.d("onConnectionStateChange newState =  $newState  ${device.address}")
                if (BluetoothGatt.STATE_CONNECTED == newState) {
                    Logger.d("onConnectionStateChange STATE_CONNECTED = $newState ,gatt == mGatt? = ${gatt == mGatt}")
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    // REFACTOR: 17/06/2017 可以设置mtu大小,若启用此方式,则请在onMtuChanged()回调成功后再搜索及发送数据,但Android之间测试发现接收方有些只能收到152个字节,暂时不考虑,后续研究
//                    Logger.d("设置mtu结果 : ${gatt.requestMtu(BlePara.mtu)}"
                    // 连接成功,开始搜索service
                    gatt.discoverServices()
                } else if (BluetoothGatt.STATE_DISCONNECTED == newState) {
                    // gatt连接断开
                    Logger.d("onConnectionStateChange STATE_DISCONNECTED = $newState")
                    gatt.close()
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                Logger.d(" mtu = $mtu  $status")
                //                    if (status == BluetoothGatt.GATT_SUCCESS) {
                //                        gatt.discoverServices();
                //                    }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                Logger.d("onServicesDiscovered status =  $status")
                val msg: Message
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    // 搜索服务成功
                    msg = mHandler.obtainMessage(MSG_TYPE_SEND_DATA_TO_THE_GATT)
                    msg.obj = gatt
                    val device = gatt.device
                    Logger.d("onServicesDiscovered 成功,开始尝试发送数据 " + device.address)
                    val services = gatt.services
                    for (service in services) {
                        Logger.d(" - service ${service.uuid}")
                    }
                } else {
                    Logger.d("onServicesDiscovered 搜索service失败,开始尝试发送数据给下一个ble设备")
                    msg = mHandler.obtainMessage(MSG_TYPE_SEND_DATA_TO_NEXT)
                    msg.obj = gatt
                }
                mHandler.sendMessage(msg)
            }
        }
    }

    /**
     * 将扫描到的 ble 设备过滤后添加到列表中,用于后续转传时使用
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun addBleDevice(result: ScanResult?) {
        if (result == null || !isSupportBle()) {
            return
        }

        // 判断是否是运行本应用程序的手机
        val scanRecord = result.scanRecord
        val device = result.device
        val address = device.address

        // 已添加过的设备不再重复添加
        if (scanRecord == null || mBleAddressSet.contains(address)) {
            return
        }

        // 若是过滤关键字为空,则记录所有扫描到的设备,否则只记录特定characteristic值符合设定的设备
        var needToAddDevice = BlePara.adCharacteristicValue.isEmpty()

        val serviceData = scanRecord.serviceData
        if (!needToAddDevice && serviceData != null && !serviceData.isEmpty()) {
            for (parcelUuid in serviceData.keys) {
                val contain = parcelUuid.uuid.toString().equals(BleConstant.AD_SERVICE_UUID_WITH_DATA, ignoreCase = true)
                if (contain) {
                    val adKeyInfo = serviceData[parcelUuid]
                    if (adKeyInfo != null && adKeyInfo.isNotEmpty()) {
                        val adInfo = String(adKeyInfo)
                        Logger.d("获取到的广播信息值为: $adInfo")
                        if (BlePara.adCharacteristicValue.equals(adInfo, true)) {
                            needToAddDevice = true
                        }
                    }
                }
            }
        }

        if (needToAddDevice) {
            mDevices.add(device)
            mBleAddressSet.add(address)
            Logger.d("record ble service successfully $address ${device.name}")
            mOnRelayListener?.onScanBleDevices(device)
        }
    }

    /**
     * 情景:
     * 1. 本轮设备连接发送超时后删除本设备并进行下一台设备的尝试
     * 2. 连接其他ble设备成功后,也成功扫描了service,但无本app指定的UUID的service,则删除本设备记录并进行下一台连接尝试
     */
    private fun skipToNextDevice() {
        mHandler.removeMessages(MSG_TYPE_SEND_DATA_TO_NEXT)
        if (mDevices.size > 0 && currentRelayBleIndex < mDevices.size) {
            // 一般是该广播信号已失效,移除该设备,后需有需要的话重新扫描
            val device = mDevices[currentRelayBleIndex]
            val address = device.address
            mBleAddressSet.remove(address)
            mDevices.removeAt(currentRelayBleIndex)
            currentRelayBleIndex -= 1
        }

        val msg = mHandler.obtainMessage(MSG_TYPE_SEND_DATA_TO_NEXT)
        mHandler.sendMessage(msg)

        mOnRelayListener?.onScanBleDevices(null)
    }

    /**
     * 定期重新扫描
     * */
    private val mStartScanRunnable = Runnable {
        mHandler.removeMessages(MSG_TYPE_AUTO_STOP_SCAN)
        val msg = mHandler.obtainMessage(MSG_TYPE_AUTO_START_SCAN)
        mHandler.sendMessage(msg)
    }

    /**
     * 用户是否开启蓝牙功能
     */
    private fun isBluetoothEnable(): Boolean {
        return mBluetoothAdapter?.isEnabled ?: false
    }

    /**
     * 创建广播设置参数
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createAdSettings(connectable: Boolean, timeoutMillis: Int): AdvertiseSettings? {
        if (!isSupportBle()) {
            return null
        }
        val mAdvertiseSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(connectable)
                .setTimeout(timeoutMillis)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()
        Logger.d("mAdvertiseSettings = $mAdvertiseSettings", TAG)
        return mAdvertiseSettings
    }


    /**
     * 创建广播数据
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createAdData(): AdvertiseData? {
        if (!isSupportBle()) {
            return null
        }
        return AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                //                .addServiceUuid(ParcelUuid.fromString(BleConstant.AD_SERVICE_UUID))
                .addServiceData(ParcelUuid.fromString(BleConstant.AD_SERVICE_UUID_WITH_DATA), BlePara.adCharacteristicValue.toByteArray())
                .build()
    }

    inner class BleBinder : Binder() {
        var onRelayListener: OnRelayListener? = null
            set(value) {
                field = value
                mGattServerCallBack.onRelayListener = value
                mOnRelayListener = value
            }


        /**
         * 开始低功耗扫描设备
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun startScanLeDevices() {
            if (isScanningBle || !isSupportBle() || mBluetoothLeScanner == null) {
                Logger.d("ble is scanning or not support ble , return ...")
                return
            }

            mHandler.removeMessages(MSG_TYPE_AUTO_STOP_SCAN)
            mHandler.removeCallbacks(mStartScanRunnable)
            mBluetoothLeScanner?.startScan(mLeScanCallback)
            isScanningBle = true
            // 自动停止扫描
            val msg = mHandler.obtainMessage(MSG_TYPE_AUTO_STOP_SCAN)
            mHandler.sendMessageDelayed(msg, AUTO_STOP_SCAN_DELAY)
            /*
             * 定制开始重新扫描ble设备
             * 目前设定是每2分钟头10s扫描,即间隔50s执行一次10s的扫描
             */
            mHandler.postDelayed(mStartScanRunnable, 120000)
        }

        /**
         * 停止扫描
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun stopScanLeDevices() {
            mHandler.removeMessages(MSG_TYPE_AUTO_STOP_SCAN)
            mBluetoothLeScanner?.stopScan(mLeScanCallback)
            isScanningBle = false
        }

        /**
         * 开启广播模式,用于本机被其他central设备搜索到
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun startAdvertising() {
            if (isBluetoothEnable()
                    && !isAdvertising
                    && isSupportAdvertisement
                    && mBluetoothLeAdvertiser != null
                    && mGattServer != null) {
                val success = mGattServerCallBack.setupServices(mGattServer)
                Logger.d("startAdvertising result  = $success ", TAG)
                if (success) {
                    mBluetoothLeAdvertiser?.startAdvertising(createAdSettings(true, 0), createAdData(), mAdCallback)
                }
            } else {
                Logger.d("startAdvertising fail", TAG)
            }
        }

        /**
         * 停止广播
         * <p>
         * ==! 貌似stop后不会回调mAdCallback,而且回调类中的方法是onStartSuccess和onStartFail....
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun stopAdvertising() {
            mBluetoothLeAdvertiser?.stopAdvertising(mAdCallback)
            isAdvertising = false
        }


        /**
         * 对数据进行转传
         *
         * @param msg 要转传的信息
         */
        fun relayData(msg: String?) {
            if (msg != null && msg.isNotEmpty()) {
                mCurrentRelayMsg = msg
                mHandler.sendEmptyMessage(MSG_TYPE_START_SEND_DATA)
            }
        }

        /**
         * 获取扫描到的符合要求的ble设备
         * */
        fun getBleDeviceList() = mDevices.toList()

        /**
         * 释放相关资源
         */
        fun release() {
        }

    }
}