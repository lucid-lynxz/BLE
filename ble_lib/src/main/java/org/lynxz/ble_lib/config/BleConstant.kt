package org.lynxz.ble_lib.config

/**
 * Created by lynxz on 19/06/2017.
 * ble一些常量参数值
 */
interface BleConstant {
    companion object {
        /**
         * 双工模式: 可收 可发
         * */
        val MODE_BOTH = 0

        /**
         * 单工模式: 只可发
         * */
        val MODE_CENTRAL_ONLY = 1

        /**
         * 单工模式: 只可收
         * */
        val MODE_PERIPHERAL_ONLY = 2

        // 默认超时设定,10s, 若转发/接收一条消息超过10s,则超时返回
        val DEFAULT_RELAY_TIME_OUT = 10 * 1000

        // 默认des加密使用的密钥(超过8字节)
        val DEFAULT_DES_KEY = "\$lynxz_ble"

        // 默认的mtu设定,若是超过20直接,则需要进行设备配置更新确认
        val DEFAULT_MTU_SIZE = 20

        val AD_SERVICE_UUID = "00001130-0000-1000-8000-00805f9b34fb"
        val AD_SERVICE_UUID_WITH_DATA = "00001131-0000-1000-8000-00805f9b34fb"

        val RELAY_SERVICE_UUID = "00001132-0000-1000-8000-00805f9b34fb"
        val RELAY_CHARACTERISTIC_UUID = "00001133-0000-1000-8000-00805f9b34fb"
    }
}