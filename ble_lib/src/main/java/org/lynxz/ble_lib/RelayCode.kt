package org.lynxz.ble_lib

/**
 * Created by lynxz on 19/06/2017.
 * 一些错误码汇总
 */
interface RelayCode {
    companion object {
        /**
         * 操作成功
         * */
        val SUCCESS = 0

        /**
         * 初始化失败 或 尚未初始化,比如context为空
         */
        val NOT_INIT = -1

        /**
         * 参数不合法
         * */
        val ERR_PARA_INVALID = -2

        /**
         * 缺少定位权限,用在ble转传功能
         * 扫描时需要定位权限且需要开启gps定位才可
         */
        val ERR_LACK_LOCATION_PERMISSION = -101

        /**
         * 系统不支持,本库只支持5.0以上
         */
        val ERR_NOT_SUPPORT = -300

        /**
         * 蓝牙未开
         */
        val ERR_BLUETOOTH_DISABLE = -301

        /**
         * gps定位未开
         */
        val ERR_GPS_DISABLE = -302
    }
}