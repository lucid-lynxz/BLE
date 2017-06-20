package org.lynxz.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.jetbrains.annotations.Nullable;
import org.lynxz.ble_lib.BleHelper;
import org.lynxz.ble_lib.callbacks.OnRelayListener;
import org.lynxz.ble_lib.config.BleConstant;
import org.lynxz.ble_lib.util.Logger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BleHelper helper = new BleHelper.Builder(this, BleConstant.Companion.getMODE_BOTH(), "hello", "$hellokotlin").build();
        helper.setOnRelayListener(new OnRelayListener() {
            @Override
            public void onReceive(@Nullable String msg) {
                Logger.d("收到蓝牙转传数据 " + msg);
            }
        });
    }

}
