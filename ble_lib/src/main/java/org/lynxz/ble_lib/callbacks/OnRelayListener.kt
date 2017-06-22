package org.lynxz.ble_lib.callbacks

import android.bluetooth.BluetoothDevice

/**
 * Created by lynxz on 19/06/2017.
 * 收到转传信息后回调
 */
interface OnRelayListener {
    /**
     * 收到转传信息
     * */
    fun onReceive(msg: String?)

    /**
     * 扫描到符合要求的ble设备时回调
     * 要求指的是: 广播中特定的serviceUUID值符合设定
     * @param bleDevices 扫描到的ble设备,若为null,表示只设备列表有更新(如删除了某设备,通知刷新列表显示)
     * */
    fun onScanBleDevices(bleDevices: BluetoothDevice?) {

    }
}