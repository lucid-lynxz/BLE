package org.lynxz.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.lynxz.ble_lib.BleHelper;
import org.lynxz.ble_lib.config.BleConstant;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BleHelper helper = new BleHelper.Builder(this, BleConstant.Companion.getMODE_BOTH(), "", "").build();
    }

}
