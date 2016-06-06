package com.coolshow.extdevicedemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.coolshow.extdevicedemo.utils.Constants;
import com.coolshow.extdevicedemo.utils.DebugLog;
import com.coolshow.extdevicedemo.utils.ToastUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author yangling
 * @version V1.0
 * @date ${date} ${time}
 * @Description:
 *
 *      扫码枪扫码的帮助类,使用时需要把广播绑定在activity的生命周期中,用于监听插入USB设备和请求权限的广播,提供了两个方法
 *
 *      registerReceiver()和unregisterReceiver();
 *
 *      如果要想对监听扫码的结果进行监听需要为其设置监听器
 *      setScanListener(ScanListener scanListener)
 *      请在启动扫码方法startScan之前进行设置,否则可能会丢失掉信息
 *
 *      请注意在manifest文件的manifest节点下设置权限:
 *
        <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
        <uses-feature android:name="android.hardware.usb.host"/>
 */

public class ScanHelper {
    private Context mContext;
    private boolean isScanConn;
    private UsbManager mManager;
    private UsbDeviceConnection connection;
    private ScanListener mScanListener;


    private static final int HANDLER_SCAN_INPUT = 1;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case HANDLER_SCAN_INPUT:
                    if(mScanListener != null){
                        mScanListener.scan(msg.obj.toString());
                    }
                    break;
            }
        }
    };
    /**
     * 设备权限的广播
     */
    private static final String ACTION_USB_PERMISSION="com.android.example.USB_PERMISSION";
    /**
     * 插入USB的广播
     */
    private static final String ACTION_USB_ATTACHED="android.hardware.usb.action.USB_DEVICE_ATTACHED";

    private static final String ACTION_USB_UNPIN="android.hardware.usb.action.USB_STATE";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DebugLog.d(action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //在这里增加通信的代码
                            startScan(device);
                        }
                    }
                }
            }else if(ACTION_USB_ATTACHED.equals(action)){
                UsbDevice device = checkScanDevice(Constants.DEVICE_VIDS,Constants.DEVICE_PIDS);
                if(device != null){
                    startScan(device);
                }else {

                }
            }else if(ACTION_USB_UNPIN.equals(action)){

            }

        }
    };

    public ScanHelper(Context context){
        mContext = context;
        isScanConn = true;

//        init();
    }


    public ScanListener getScanListener() {
        return mScanListener;
    }

    public void setScanListener(ScanListener scanListener) {
        mScanListener = scanListener;
    }


    public void registerReceiver() {
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_UNPIN);
        mContext.registerReceiver(mUsbReceiver, filter);
    }

    public void unregisterReceiver(){
        mContext.unregisterReceiver(mUsbReceiver);
    }

    /**
     * 检测是否有我们所需要的设备
     * @return
     */
    public UsbDevice checkScanDevice(int[] vids,int[] pids) {
        if(vids.length != pids.length){
            ToastUtils.showToast(mContext,"vids与pids的数量不匹配!");
            return null;
        }
        if(vids == null || pids == null){
            ToastUtils.showToast(mContext,"vids或pids不能为空");
            return null;
        }
        mManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> devices = mManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = devices.values().iterator();
        int count = devices.size();
        if (count > 0) {
            while (deviceIterator.hasNext()) {
                UsbDevice dev = deviceIterator.next();
                int pid = dev.getProductId();
                int vid = dev.getVendorId();
                if (hasDevice(vids,pids,vid,pid)) {
                    if(!mManager.hasPermission(dev)){
                        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                        mManager.requestPermission(dev,mPermissionIntent);
                        return null;
                    }
                    return dev;
                }
            }

        }
        return null;
    }

    private boolean hasDevice(int[] vids,int[] pids,int vid,int pid){
        for (int i = 0; i < vids.length; i++) {
            if(vids[i] == vid && pids[i] == pid){
                return true;
            }
        }

        return false;
    }

    /**
     * 启动扫码
     * @param device
     */
    public void startScan(final UsbDevice device){
        if(device == null)
            return;
        isScanConn = true;
        new Thread(){
            @Override
            public void run() {


                while (isScanConn){
                    UsbInterface intf= device.getInterface(0);
                    UsbEndpoint endpoint= intf.getEndpoint(0);
                    connection= mManager.openDevice(device);
                    if(connection == null){
                        Toast.makeText(mContext,"不能打开连接!",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    connection.claimInterface(intf, true);
                    if(device != null){
                        byte[] bytes = new byte[512];
                        connection.bulkTransfer(endpoint, bytes, bytes.length, 0);//do in another thread
                        int len = 2;
                        for (;len < bytes.length ; len++){
                            if(bytes[len] == '\r'){
                                break;
                            }
                        }
                        if(len >= bytes.length)return;
                        String str =  new String(bytes,2,len-2);
                        Message message = Message.obtain();
                        message.what = HANDLER_SCAN_INPUT;
                        message.obj = str;
                        mHandler.sendMessage(message);
                        if(connection != null)
                            connection.close();
                    }


                }
            }
        }.start();
    }

    public void stopScan(){
        if(connection != null){
            connection.close();
        }
        isScanConn = false;
    }

    public interface ScanListener{
        void scan(String data);
    }
}
