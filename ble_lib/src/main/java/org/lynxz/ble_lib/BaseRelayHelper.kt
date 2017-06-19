package org.lynxz.ble_lib

import android.content.Context
import org.lynxz.ble_lib.callbacks.OnRelayListener

/**
 * Created by lynxz on 19/06/2017.
 * 转传通讯帮助类
 */
abstract class BaseRelayHelper {
    companion object {
        val TAG = "BaseRelayHelper"
    }

    /**
     * 用于 收到 or 发送 信息时回调给app
     * */
    var onRelayListener: OnRelayListener? = null

    /**
     * 进行一些初始化操作用
     * */
    abstract fun init(context: Context): Int

    /**
     * 转传数据
     * */
    abstract fun relayData(msg: String?): Int

    fun release() {
        onRelayListener = null
    }
}