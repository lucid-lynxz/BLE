package org.lynxz.ble

import android.Manifest
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
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
                    tv_info.text = "收到蓝牙转传数据:\n$msg"
                }
            }
        }

        btn_start_scan.setOnClickListener { BleHelper.startScan() }
        btn_stop_scan.setOnClickListener { BleHelper.stopScan() }
        btn_start_advertising.setOnClickListener { BleHelper.startAdvertising() }
        btn_stop_advertising.setOnClickListener { BleHelper.stopAdvertising() }
        btn_send.setOnClickListener {
            val msg = edt_relay_info.text.toString()
            if (msg.isEmpty()) {
                showToast("请输入内容后重试")
            } else {
                BleHelper.relayData(msg)
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
