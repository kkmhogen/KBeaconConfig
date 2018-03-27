/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beacon.kbeacon;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BeaconPerpMgr {
    public final static int MAX_CONN_TIME_FOR_ONE_STEP = 20 * 1000;
    public final static int MSG_CONN_TIMEOUT = 2003;
    public final static int MSG_CONN_SUCCESS = 2004;
    public final static int MSG_EXTEND_CONN_TIMEOUT = 2005;

    private final static String TAG = "Beacon.PerpMgr";//BeaconPerpMgr.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private  static int nTrackNameIndex = 0;
    private long mLastCheckAliveTick = 0;

    private HashMap<String, BeaconPerperal> mLeDevicesMap;
    private ArrayList<String> mBleDeviceMacArray = new ArrayList<>(50);
    private ArrayList<String> mBleDeviceSorted = new ArrayList<>(50);

    private Activity mContext;


    static private BeaconPerpMgr sBleMgr;
    static BeaconPerpMgr shareInstance(Activity ctx)
    {
        if (sBleMgr == null){
            sBleMgr = new BeaconPerpMgr(ctx);

            if (!sBleMgr.initialize()){
                sBleMgr = null;
            }
        }

        return sBleMgr;
    }

    public android.os.Handler mHandler = new MsgHandler();

    private class RssiComparator implements Comparator {
        public int compare(Object o1,Object o2) {
            String strMac1 = (String)o1;
            String strMac2 = (String)o2;
            BeaconPerperal device1 = mLeDevicesMap.get(strMac1);
            BeaconPerperal device2 = mLeDevicesMap.get(strMac2);
            if(device1.getAdvRssiInt() > device2.getAdvRssiInt()) {
                return -1;
            } else if (device1.getAdvRssiInt() == device2.getAdvRssiInt()) {
                return 0;
            } else {
                return 1;
            }
        }

    }

    public boolean filterByName(String strFilterName)
    {
        if (strFilterName == null || strFilterName.equals("")){
            mBleDeviceSorted.clear();
            for (int i = 0; i < mBleDeviceMacArray.size(); i++){
                String strMacAddr = mBleDeviceMacArray.get(i);
                mBleDeviceSorted.add(strMacAddr);
            }

            return true;
        }

        mBleDeviceSorted.clear();
        for (int i = 0; i < mBleDeviceMacArray.size(); i++){
            String strMacAddr = mBleDeviceMacArray.get(i);
            BeaconPerperal beaconPerp = mLeDevicesMap.get(strMacAddr);
            String strDevName = beaconPerp.getName();
            if (strDevName != null)
            {
                strDevName = strDevName.toLowerCase();
                strFilterName = strFilterName.toLowerCase();
                if (strDevName.contains(strFilterName)) {
                    mBleDeviceSorted.add(strMacAddr);
                }
            }
        }

        return true;
    }

    public boolean sortDevByRSSI()
    {
        RssiComparator rssiSort = new RssiComparator();
        Collections.sort(mBleDeviceSorted, rssiSort) ;

        return true;
    }

    private class MsgHandler extends  android.os.Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_CONN_SUCCESS:{
                    String strMac = (String)msg.obj;
                    if (mHandler.hasMessages(MSG_CONN_TIMEOUT, strMac)) {
                        mHandler.removeMessages(MSG_CONN_TIMEOUT);
                    }
                    break;
                }

                case MSG_CONN_TIMEOUT:{
                    String strMacAddr = (String)msg.obj;
                    if (strMacAddr != null){
                        handleConnTimeout(strMacAddr);
                    }
                    break;
                }

                case MSG_EXTEND_CONN_TIMEOUT:{
                    String strMac = (String)msg.obj;
                    if (strMac != null) {
                        if (mHandler.hasMessages(MSG_CONN_TIMEOUT, strMac)) {
                            mHandler.removeMessages(MSG_CONN_TIMEOUT);
                        }

                        Message newMsg = mHandler.obtainMessage(MSG_CONN_TIMEOUT);
                        newMsg.obj = strMac;
                        mHandler.sendMessageDelayed(newMsg, MAX_CONN_TIME_FOR_ONE_STEP);
                    }
                    break;
                }

                default:
                    break;
            }
        }
    };

	private BeaconPerpMgr(Activity c){
		mContext = c;
	}

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mLeDevicesMap = new HashMap<String, BeaconPerperal>();

        return true;
    }

    public BeaconPerperal createBlePerp(BluetoothDevice device)
    {
        //check if is null
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Conn failed, BluetoothAdapter not initialized or unspecified address.");
            return null;
        }

        // Previously connected device.  Try to reconnect.
        long currTick = System.currentTimeMillis();
        BeaconPerperal blePerp = mLeDevicesMap.get(device.getAddress());
        if (blePerp != null)
        {
            Log.d(TAG, "Found existing TIBLE instance for connection.");
            blePerp.mAdvLastUpdateTick = currTick;
        }
        else {
            //创建实例
            Log.d(TAG, "Trying to create an new TIBLE instance for connection.");
            blePerp = new BeaconPerperal(mBluetoothAdapter, mContext, mHandler, device);

            //update time
            blePerp.mAdvLastUpdateTick = currTick;
            mLeDevicesMap.put(blePerp.getMacAddress(), blePerp);
            mBleDeviceMacArray.add(blePerp.getMacAddress());

            mBleDeviceSorted.add(blePerp.getMacAddress());
        }
        return blePerp;
    }



    public void startConnect(final String strMacAddr) {
        //check if is null
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Conn failed, BluetoothAdapter not initialized or unspecified address.");
            return;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        final BeaconPerperal blePerp = mLeDevicesMap.get(strMacAddr);
        if (blePerp != null && blePerp.isDisconnected()) {
            blePerp.startConnect();

            Log.d(TAG, "Start connect to  " + strMacAddr);

            //start conn timeout timer
            mHandler.removeMessages(MSG_CONN_TIMEOUT);
            Message msg = mHandler.obtainMessage(MSG_CONN_TIMEOUT);
            msg.obj = blePerp.getMacAddress();
            mHandler.sendMessageDelayed(msg, MAX_CONN_TIME_FOR_ONE_STEP);
        }
    }

    void handleConnTimeout(String strMacAddr){
        final BeaconPerperal blePerp = mLeDevicesMap.get(strMacAddr);
        if (blePerp == null){
            return;
        }

        if (!blePerp.isConnected()) {

            Log.e(TAG, "handleConnTimeout: Conn to beacon timeout.");

            blePerp.closeConnection();
        }
    }


    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void closeAllConnection() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for (Object key : mLeDevicesMap.keySet()) {
            BeaconPerperal blePerp = mLeDevicesMap.get(key);
            blePerp.closeConnection();
        }
        mLeDevicesMap.clear();
        mBleDeviceMacArray.clear();
        mBleDeviceSorted.clear();
    }

    public BeaconPerperal getTrackByMac(String strMacAddr){
        return mLeDevicesMap.get(strMacAddr);
    }

    public int getDeviceArrSize()
    {
        return mBleDeviceSorted.size();
    }

    public BeaconPerperal getTrackByMac(int nIndex){
        String strMacAddr = mBleDeviceSorted.get(nIndex);
        return mLeDevicesMap.get(strMacAddr);
    }

    public int getPositionByMac(String strMmacAddr){
        for (int i = 0; i < mBleDeviceSorted.size(); i++)
        {
            if (mBleDeviceSorted.get(i).equals(strMmacAddr)){
                return i;
            }
        }

        return -1;
    }

}
