package org.lynxz.ble

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import org.lynxz.ble_lib.BleHelper
import org.lynxz.ble_lib.callbacks.OnRelayListener
import org.lynxz.ble_lib.config.BleConstant
import org.lynxz.ble_lib.showToast
import org.lynxz.ble_lib.util.Logger

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

        with(BleHelper) {
            init(this@MainActivity)
            val updatePara = updatePara(this@MainActivity, BleConstant.MODE_BOTH, "hello", null)
            Logger.d("更新ble参数结果: $updatePara")
            onRelayListener = object : OnRelayListener {
                override fun onReceive(msg: String?) {
                    Logger.d("收到蓝牙转传数据 $msg")
                }
            }

        }
    }

    /**
     * 扫描时需要定位权限
     * */
    private fun requestPermission() {
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe { accept ->
                    Logger.d("request result = " + accept!!)
                    if (!accept) {
                        showToast("ble转传功能需要定位权限,否则可能扫描不到设备")
                    } else {
                        BleHelper.start()
                        Logger.d("开启ble功能")
                    }
                }
    }
}
