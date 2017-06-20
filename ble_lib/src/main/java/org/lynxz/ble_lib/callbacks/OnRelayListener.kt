package org.lynxz.ble_lib.callbacks

/**
 * Created by lynxz on 19/06/2017.
 * 收到转传信息后回调
 */
interface OnRelayListener {
    /**
     * 收到转传信息
     * */
    fun onReceive(msg: String?)
}