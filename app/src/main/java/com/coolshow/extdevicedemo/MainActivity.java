package com.coolshow.extdevicedemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.coolshow.extdevicedemo.utils.Constants;

public class MainActivity extends AppCompatActivity {

    private TextView mTv;

    private ScanHelper mScanHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTv = (TextView) findViewById(R.id.tv);
        init();

    }

    private void init() {
        mScanHelper = new ScanHelper(this);
        mScanHelper.setScanListener(new ScanHelper.ScanListener() {
            @Override
            public void scan(String data) {
                mTv.setText(data);
            }
        });
        mScanHelper.registerReceiver();
        mScanHelper.startScan(mScanHelper.checkScanDevice(Constants.DEVICE_VIDS,Constants.DEVICE_PIDS));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanHelper.unregisterReceiver();
        mScanHelper.stopScan();
    }



}
