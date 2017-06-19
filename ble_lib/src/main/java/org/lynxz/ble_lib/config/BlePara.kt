package org.lynxz.ble_lib.config

/**
 * Created by lynxz on 19/06/2017.
 * 用户可设定的一些参数
 */
class BlePara {
    companion object {
        var mode = BleConstant.MODE_BOTH // 默认为可收也可发
        var relayTimeout = BleConstant.DEFAULT_RELAY_TIME_OUT // 接收/发送超时设定
        var desKey = BleConstant.DEFAULT_DES_KEY // 开启des后,默认的密钥
        var adCharacteristicValue = "" // 默认广播模式下,其他设备可识别的名称,用于过滤设备,默认不过滤
        var mtu = BleConstant.DEFAULT_MTU_SIZE // mtu设置
    }
}