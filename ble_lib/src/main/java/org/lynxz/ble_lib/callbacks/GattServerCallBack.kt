package org.lynxz.ble_lib.callbacks

import android.bluetooth.BluetoothGattServerCallback

/**
 * Created by lynxz on 19/06/2017.
 *
 */
class GattServerCallBack : BluetoothGattServerCallback() {

    var onRelayListener: OnRelayListener? = null
}