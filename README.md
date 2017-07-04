# BLE
android 低功耗蓝牙封装库(5.0以上),支持 central 和 peripheral 模式

模式: 可收/可发   只可收(peripheral模式) 只可发(central)模式
可设定超时, des加密, crc校验功能等
接收成功后,只管通知给app,不对信息做过滤(如是否是跟前一条消息内容一致等),由app自行判断

### 要求
1. 手机开启蓝牙和gps定位;
2. app拥有 `Manifest.permission.ACCESS_FINE_LOCATION` 权限;

### 使用
1. 导包
```gradle
compile 'org.lynxz.ble:ble:0.1.1'
```

2. 代码调用
```kotlin
// 初始化及参数设定
with(BleHelper) {
    // 初始化
    init(this@MainActivity)

    // 设定模式参数
    val keyInfo = "hello" // 广播关键字,用于扫描时过滤, 尽量10个字节以内

    // 当前支持的三种模式: 
    //   BleConstant.MODE_BOTH -> 同时作为central和peripheral设备
    //   BleConstant. MODE_CENTRAL_ONLY -> 只作为central设备,只可发
    //   BleConstant. MODE_PERIPHERAL_ONLY -> 只作为peripheral设备,只可收
    val updatePara = updatePara(this@MainActivity, BleConstant.MODE_BOTH, keyInfo, null)

    // 设置回调通知
    onRelayListener = object : OnRelayListener {
        override fun onReceive(msg: String?) {
            // app收到蓝牙数据
        }

        /**
            * 扫描到符合要求的ble设备时回调
            * 要求指的是: 广播中特定的serviceUUID值符合设定
            * @param bleDevices 扫描到的ble设备,若为null,表示只设备列表有更新(如删除了某设备,通知刷新列表显示)
            * */
        override fun onScanBleDevices(bleDevices: BluetoothDevice?) {
            super.onScanBleDevices(bleDevices)
        }
    }
}
```