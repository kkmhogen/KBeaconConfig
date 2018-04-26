package com.beacon.kbeacon;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by hogen on 6/26/15.
 */
public class BeaconPerperal {

    public final static String BEACON_MAC_ADDRESS = "BeaconPerperal.BEACON_MAC_ADDRESS";

    public final static String BEACON_CONN_STATUS_CHG = "BeaconPerperal.BEACON_CONN_STATUS_CHG";
    public final static String BEACON_NEW_CONN_STATUS = "BeaconPerperal.BEACON_NEW_CONN_STATUS";
    public final static String BEACON_OLD_CONN_STATUS = "BeaconPerperal.BEACON_OLD_CONN_STATUS";

    public final static String BEACON_APP_CHG = "BeaconPerperal.BEACON_APP_CHG";
    public final static String BEACON_APP_VALUE = "BeaconPerperal.BEACON_APP_VALUE";
    public final static String BEACON_EXT_VALUE = "BeaconPerperal.BEACON_EXT_VALUE";

    //connect status
    public final static int BEACON_CONN_STATUS_INVALID = 0;
    public final static int BEACON_CONN_STATUS_CONNECTING = 1;
    public final static int BEACON_CONN_STATUS_DISCOV_SRV = 2;
    public final static int BEACON_CONN_STATUS_CONNECTED = 3;
    public final static int BEACON_CONN_STATUS_DISCONNECTED = 4;

    //beacon status
    public final static int BEACON_NTF_INVALID = 0;
    public final static int BEACON_NTF_AUTH_FAILED = 4;
    public final static int BEACON_NTF_MO_CFG_CHANGE = 5;
    public final static int BEACON_NTF_WR_COMPLETE = 6;
    public final static int BEACON_NTF_RD_COMPLETE = 7;
    public final static int BEACON_NTF_WR_READ_CHECK_FAIL= 8;
    public final static int BEACON_NTF_DEV_UNKNOWN = 9;
    public final static int BEACON_NTF_WR_UNLOCK_CMP_PROGRESS = 10;
    public final static int BEACON_NTF_WR_CFG_PROGRESS = 11;
    public final static int BEACON_NTF_RD_CFG_PROGRESS = 12;

    public final static int MAX_RMV_TICK = 180 * 1000;

    public final static int MAX_CONN_TIME = 15 * 1000;

    public android.os.Handler mHandler = new MsgHandler();

    private final static int MSG_CLOSE_CONNECTION = 0x199;
    private final static int MSG_CONNECT_DEVICE_TIMEOUT = 0x198;
    private final static int MSG_START_CONNECT_DEVICE = 0x20;
    private final static int MSG_START_RECONNECT_DEVICE = 0x201;
    private final static int MSG_BEACON_DISCOVER_SUCCESS = 0x202;
    private final static int MSG_BEACON_START_READ_PARA = 0x203;
    private final static int MSG_READ_CHAR_COMPLETE  = 0x204;
    private final static int MSG_BEACON_START_WRITE_PARA  = 0x205;
    private final static int MSG_WRITE_CHAR_COMPLETE  = 0x206;
    private final static int MSG_START_DISCOVER_SRV  = 0x207;
    private final static int MSG_UNLOCK_DEVICE_COMPLETE  = 0x208;



    private final static String TAG = "Beacon.Perpheral";

    public final static int INVALID_INT_VALUE = 0xFFFFFFFF;

    public final static int EDDYSTONE_MAX_URL_ENCODE_LEN = 18;
    public final static int EDDYSTONE_MAX_URL_DECODE_LEN = 40;

    public final static int BEACON_TYPE_EDDY_UID = 0x0;
    public final static int BEACON_TYPE_EDDY_URL = 0x10;
    public final static int BEACON_TYPE_EDDY_TLM = 0x20;
    public final static int BEACON_TYPE_IBEACON = 0x30;
    public final static int BEACON_TYPE_CFG_MODE = 0x40;
    public final static int BEACON_TYPE_INVALID = 0xFF;

    public final static int MIN_EDDY_TLM_LEN = 12+1;
    public final static int MIN_EDDY_UID_LEN = 1+1+16;
    public final static int MIN_EDDY_URL_LEN = 1+1+1;

    //connection info
    private String mAdvName;  //name
    public String mMacAddress;  //mac address
    private boolean mConnEnable = false;

    //beacon type
    int mAdvBeaconType = BEACON_TYPE_INVALID;

    //advtisement data
    private int mAdvBatteryLevel = INVALID_INT_VALUE;
    private float mAdvTemputure = INVALID_INT_VALUE;
    private int mAdvCount = 0;
    private int mAdvTxPowerLevel = INVALID_INT_VALUE;
    private int mAdvType = INVALID_INT_VALUE;
    private int mAdvRssiResult = 0;

    //apple ibeacon data
    private String mAdvIBeaconUUID;
    private String mAdvIBeaconMajorID;
    private String mAdvIBeaconMinorID;

    //Eddystone URL
    public String mAdvEddystoneUrl;


    //Eddystone UID
    public String mAdvEddystoneNamesapceID;
    public String mAdvEddystoneSerialID;

    //test if the rssi > minal
    public long mAdvFindDevTick = 0;
    public long mAdvLastUpdateTick = 0;
    public long mAdvLastTLMUpdateTick = 0;
    public int mAdvRttTick = 1000;
    public int mAdvTLMRttTick = 0;

    private BluetoothGatt mBleGatt;
    private int mBleConnStatus = BEACON_CONN_STATUS_DISCONNECTED;    //BLE connection state
    private int mReConnNum = 0;

    //config data from profile
    private byte[] mCfgUnlockCode = {'0','0','0','0','0','0','0','0','0','0','0','0','0','0','0','0'};
    private byte[] mCfgNewLockCode = {'0','0','0','0','0','0','0','0','0','0','0','0','0','0','0','0'};

    //configruation
    private boolean mCfgBeaconLockstatus = false;
    private byte[] mCfgBeaconTxPowerLvls = new byte[4];
    private byte mCfgBeaconTxIdx = 0;
    private int mCfgBeaconAdvPeriod = 0;
    private byte mCfgAdvFlag = 0;
    private byte mCfgAdvType = 0;
    private String mCfgDevName  = "";
    private String mCfgDevSerialID = "";

    //ibeacon info
    private byte[]mCfgIBeaconUUID = new byte[16];
    private byte[]mCfgIBeaconMajorID = new byte[2];
    private byte[]mCfgIBeaconMinorID = new byte[2];

    //eddystone url data
    private byte[] mCfgEddyUriData = null;
    //eddystone uid
    private byte[]mCfgEddyNamespaceID = new byte[10];
    private byte[]mCfgEddySerialID = new byte[6];

    //config operation
    private ArrayList<java.util.UUID> mReadCharacterList = new ArrayList<>(10);
    private ArrayList<java.util.UUID> mReadErrorCharacterList = new ArrayList<>(10);
    private ArrayList<java.util.UUID> mWriteCharacterList = new ArrayList<>(10);
    private int mNextReadUUIDIndex = 0;
    private int mNextWriteUUIDIndex = 0;

    private enum eActionPendingStatus
    {
        ActionPendingInitReadCfg,

        ActionPendingWriteCfg,

        ActionPendingIReadCheck,

        ActionPendingNULL,
    };
    private eActionPendingStatus mActionPendingStatus;


    //key for notify
    private int mKeyNotify = 0;   //key press notify:  0: not notify;  1: press notify;   2: long press notify


    //mgr
    private Handler mParentHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private Activity mContext;
    SharePreferenceMgr mPref;

    public BeaconPerperal(BluetoothAdapter adapter, Activity context, Handler parentHandler, BluetoothDevice device) {
        mBluetoothAdapter = adapter;
        mContext = context;
        mParentHandler = parentHandler;
        mPref = SharePreferenceMgr.shareInstance(context);


        mMacAddress = device.getAddress();
        mAdvName = device.getName();// + "_" + String.valueOf(nTrackNameIndex++);

        mAdvFindDevTick = System.currentTimeMillis();

        initReadDataList();

        //conn status
        mAdvRssiResult = 0;
        mReConnNum = 0;
    }

    private void initReadDataList()
    {
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_LOCK_STATE_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_URI_DATA_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_ADV_FLAG_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_TX_POWER_LVL_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_TX_POWER_LVL_IDX_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_ADV_PERIOD_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_UID_DATA_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_ADV_TYPE_UUID);
        mReadCharacterList.add(Utils.CHR_BEACON_CFG_DEV_NAME_UUID);
    }

    public void updateIBeaconInfo(String uuid, String majorID, String minorID, int nPower)
    {
        mAdvBeaconType = BEACON_TYPE_IBEACON;
        mAdvIBeaconUUID = uuid;
        mAdvIBeaconMajorID = majorID;
        mAdvIBeaconMinorID = minorID;
        mAdvTxPowerLevel = nPower;
    }

    public void updateEddystoneTLM(int nBatteryLevel, float fTemputure, int nAdvCount)
    {
        mAdvBeaconType = BEACON_TYPE_EDDY_TLM;
        mAdvBatteryLevel = nBatteryLevel;
        mAdvTemputure = fTemputure;

        long nCurrMill = System.currentTimeMillis();
        float nLastCount = nAdvCount - mAdvCount;
        if (mAdvCount != 0 && mAdvLastTLMUpdateTick != 0 && nLastCount != 0)
        {
            float nLastTick = nCurrMill - mAdvLastTLMUpdateTick;
            int nLastRttTick =  (int)(nLastTick / nLastCount);
            mAdvTLMRttTick = (int)((float)(mAdvTLMRttTick * 0.3) + (float)(nLastRttTick*0.7));
        }
        mAdvCount = nAdvCount;
        mAdvLastTLMUpdateTick = System.currentTimeMillis();
    }

    public void updateConfigMode(String strDevName)
    {
        mAdvName = strDevName;
        mAdvBeaconType = BEACON_TYPE_CFG_MODE;
    }

    public void updateEddystoneURL(String url, int nPower)
    {
        mAdvBeaconType = BEACON_TYPE_EDDY_URL;
        mAdvEddystoneUrl = url;
        mAdvTxPowerLevel = nPower;
    }

    public void updateUidInfo(String namespaceID, String serialID, int nPower)
    {
        mAdvBeaconType = BEACON_TYPE_EDDY_UID;
        mAdvEddystoneNamesapceID = namespaceID;
        mAdvEddystoneSerialID = serialID;
        mAdvTxPowerLevel = nPower;
    }

    public String getName()
    {
        String strBeaconName = mAdvName;
        if (strBeaconName == null){
            strBeaconName = "N/A";
        }

        if (mAdvBeaconType == BEACON_TYPE_EDDY_URL){
            strBeaconName += "(EddystoneUrl)";
        }
        else if (mAdvBeaconType == BEACON_TYPE_EDDY_UID){
            strBeaconName += "(EddystoneUID)";
        }
        else if (mAdvBeaconType == BEACON_TYPE_EDDY_TLM){
            strBeaconName += "(EddystoneTLM)";
        }
        else if (mAdvBeaconType == BEACON_TYPE_IBEACON) {
            strBeaconName += "(iBeacon)";
        }

        return strBeaconName;
    }

    public String getDeviceName()
    {
        String strBeaconName = mAdvName;
        if (strBeaconName == null){
            strBeaconName = "N/A";
        }

        return strBeaconName;
    }

    private class MsgHandler extends  android.os.Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_CONNECT_DEVICE:{
                    mHandler.removeMessages(MSG_START_CONNECT_DEVICE);
                    startConnDev();
                    break;
                }

                case MSG_START_DISCOVER_SRV:{
                    mHandler.removeMessages(MSG_START_DISCOVER_SRV);
                    if (mBleGatt != null){
                        mBleGatt.discoverServices();
                    }
                    break;
                }

                case MSG_CONNECT_DEVICE_TIMEOUT:{
                    mHandler.removeMessages(MSG_CONNECT_DEVICE_TIMEOUT);
                    Log.e(TAG, "MSG_CONNECT_DEVICE_TIMEOUT.");
                    handleConnTimeout();
                    break;
                }

                case MSG_START_RECONNECT_DEVICE:{
                    mHandler.removeMessages(MSG_START_RECONNECT_DEVICE);
                    startReConnectDev();
                    break;
                }

                case MSG_BEACON_DISCOVER_SUCCESS:{
                    //start read para
                    mHandler.removeMessages(MSG_BEACON_DISCOVER_SUCCESS);
                    //notify
                    int nOldConnState = mBleConnStatus;
                    mBleConnStatus = BEACON_CONN_STATUS_DISCOV_SRV;
                    sendConnChgBroadcast(nOldConnState, mBleConnStatus);

                    //input verify code
                    mActionPendingStatus = eActionPendingStatus.ActionPendingInitReadCfg;
                    startWriteDeviceInfo(Utils.CHR_BEACON_CFG_UNLOCK_UUID);
                }
                break;

                //start read param
                case MSG_BEACON_START_READ_PARA: {
                    java.util.UUID uuid = (java.util.UUID)msg.obj;
                    mHandler.removeMessages(MSG_BEACON_START_READ_PARA);
                    startReadDeviceInfo(uuid);
                    break;
                }

                case MSG_READ_CHAR_COMPLETE:{
                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingInitReadCfg) {
                        if (mBleConnStatus == BEACON_CONN_STATUS_DISCOV_SRV) {
                            handleConnSucc();
                        }else{
                            sendAppChgBroadcast(BEACON_NTF_RD_COMPLETE, 0);
                        }
                    }
                    else if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck){
                        if (mReadErrorCharacterList.size() > 0){
                            sendAppChgBroadcast(BEACON_NTF_WR_READ_CHECK_FAIL, 0);
                        }else {
                            sendAppChgBroadcast(BEACON_NTF_WR_COMPLETE, 0);
                        }
                    }
                    mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;
                    break;
                }

                //start save paramaters
                case MSG_BEACON_START_WRITE_PARA:{
                    java.util.UUID uuid = (java.util.UUID)msg.obj;
                    mHandler.removeMessages(MSG_BEACON_START_WRITE_PARA);
                    startWriteDeviceInfo(uuid);
                    break;
                }

                case MSG_WRITE_CHAR_COMPLETE:{
                    mActionPendingStatus = eActionPendingStatus.ActionPendingIReadCheck;

                    //start read check
                    mReadErrorCharacterList.clear();
                    mNextReadUUIDIndex = 0;

                    sendReadNextBeaconInfoReq();
                    break;
                }

                case MSG_CLOSE_CONNECTION:{
                    if (mReConnNum == 0 &&
                            (mBleConnStatus == BEACON_CONN_STATUS_CONNECTING
                            || mBleConnStatus == BEACON_CONN_STATUS_DISCOV_SRV))
                    {
                        closeForQuickReconnect();
                        mHandler.sendEmptyMessageDelayed(MSG_START_RECONNECT_DEVICE, 300);
                    }else{
                        closeConnection();
                    }
                    break;
                }

                default:
                    break;
            }
        }
    };


    public boolean isAddedTimeout(long currTick) {
        long nAliveTick = mPref.getAliveTick() * 1000;
        if (currTick - mAdvFindDevTick > nAliveTick) {
            return true;
        }
        return false;
    }

    public boolean isUpdateTimeout(long currTick) {
        if (currTick - mAdvLastUpdateTick > MAX_RMV_TICK) {
            return true;
        }
        return false;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BluetoothDevice tmpBlePerp = gatt.getDevice();
            if (!mMacAddress.equals(tmpBlePerp.getAddress())) {
                return;
            }

            if (mBleGatt != gatt) {
                Log.e(TAG, "update gatt in BluetoothGattCallback");
                mBleGatt = gatt; //更新
            }

            Log.i(TAG, String.format("Mac:%s onConnectionStateChange for connection,old state:%d new state:%d",
                    mMacAddress,
                    mBleConnStatus, newState));

            //check if result is success
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange failed:" + mMacAddress);
                mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
            } else {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, mMacAddress + ": connect success, now start discover.");

                   mHandler.sendEmptyMessageDelayed(MSG_START_DISCOVER_SRV, 1200);
                    extendConnTimeout();
                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    Log.i(TAG, "Connecting to GATT server.");
                } else {
                    Log.i(TAG, "Disconnected from GATT server." + mMacAddress);

                    //reconnect
                    mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {


            BluetoothDevice tmpBlePerp = gatt.getDevice();
            if (!mMacAddress.equals(tmpBlePerp.getAddress())) {
                Log.w(TAG, "onServicesDiscovered mac address error: " + status);
                return;
            }

            //检查是否更新
            if (mBleGatt != gatt) {
                //Log.i(TAG, "update gatt in onServicesDiscovered");
                Log.w(TAG, "onServicesDiscovered update gatt " + status);
                mBleGatt = gatt; //更新
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered success: " + status);

                //read broadcast info
                mHandler.sendEmptyMessage(MSG_BEACON_DISCOVER_SUCCESS);

                extendConnTimeout();
            } else {
                Log.w(TAG, "onServicesDiscovered failed: " + status);

                mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "onCharRead " + gatt.getDevice().getName()
                        + " read "
                        + characteristic.getUuid().toString()
                        + " -> "
                        + Utils.bytesToHexString(characteristic.getValue()));

                UUID cId = characteristic.getUuid();
                UUID sId = characteristic.getService().getUuid();
                if (sId == null || cId == null) {
                    Log.e(TAG, mMacAddress + ":onCharacteristicRead get serviceUuid null");
                    return;

                }

                if (mBleConnStatus == BEACON_CONN_STATUS_DISCOV_SRV) {
                    extendConnTimeout();
                }

                byte byReadValue[] = characteristic.getValue();
                if (cId.equals(Utils.CHR_BEACON_CFG_LOCK_STATE_UUID)) {
                    if (byReadValue.length < 1){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }
                    mCfgBeaconLockstatus = byReadValue[0] > 0;
                   if (mActionPendingStatus == eActionPendingStatus.ActionPendingInitReadCfg) {
                        if (mCfgBeaconLockstatus) {

                            Log.e(TAG, mMacAddress + ":onCharacteristicRead unlock failed");
                            mReConnNum++;  //no need reconnect
                            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);

                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendAppChgBroadcast(BEACON_NTF_AUTH_FAILED, 10);
                                }
                            }, 500);

                            return;
                        }
                    }
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get lock status:" + mCfgBeaconLockstatus);
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_URI_DATA_UUID)) {
                    if (byReadValue.length < 1 || byReadValue.length > EDDYSTONE_MAX_URL_ENCODE_LEN){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (!Utils.isByteValueEqual(mCfgEddyUriData, byReadValue)){
                            Log.e(TAG, "Write URI data failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }
                    mCfgEddyUriData = new byte[byReadValue.length];
                    System.arraycopy(byReadValue, 0, mCfgEddyUriData, 0, byReadValue.length);

                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get uri data:" + byReadValue.toString());
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_ADV_FLAG_UUID)) {
                    if (byReadValue.length < 1){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (mCfgAdvFlag != byReadValue[0]){
                            Log.e(TAG, "Write cfg adv flag failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgAdvFlag = byReadValue[0];
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get adv flag:" + mCfgAdvFlag);
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_TX_POWER_LVL_UUID)) {
                    if (byReadValue.length < 4){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (!Utils.isByteValueEqual(mCfgBeaconTxPowerLvls, byReadValue)){
                            Log.e(TAG, "Write cfg tx power lvls failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    System.arraycopy(byReadValue, 0, mCfgBeaconTxPowerLvls, 0, 4);
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get tx power lvls:" + mCfgBeaconTxPowerLvls.toString());
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_TX_POWER_LVL_IDX_UUID)) {
                    if (byReadValue.length < 1){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (mCfgBeaconTxIdx != byReadValue[0]){
                            Log.e(TAG, "Write cfg tx power idx failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgBeaconTxIdx = byReadValue[0];
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get tx power idx:" + mCfgBeaconTxIdx);
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_ADV_PERIOD_UUID)) {
                    if (byReadValue.length < 2){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }
                    int nTempCfgAdvPeriod = (byReadValue[1] & 0xFF);
                    nTempCfgAdvPeriod = nTempCfgAdvPeriod << 8;
                    nTempCfgAdvPeriod += (byReadValue[0] & 0xFF);
                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (mCfgBeaconAdvPeriod != nTempCfgAdvPeriod){
                            Log.e(TAG, "Write cfg adv period failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgBeaconAdvPeriod = nTempCfgAdvPeriod;
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get adv period:" + mCfgBeaconAdvPeriod);
                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID)) {
                    if (byReadValue.length < 16+4){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    byte[]tmpIBeaconUUID = new byte[16];
                    byte[]tmpIBeaconMajorID = new byte[2];
                    byte[]tmpIBeaconMinorID = new byte[2];
                    System.arraycopy(byReadValue, 0, tmpIBeaconUUID, 0, 16);
                    System.arraycopy(byReadValue, 16, tmpIBeaconMajorID, 0, 2);
                    System.arraycopy(byReadValue, 18, tmpIBeaconMinorID, 0, 2);

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (!Utils.isByteValueEqual(mCfgIBeaconUUID, tmpIBeaconUUID)
                                || !Utils.isByteValueEqual(mCfgIBeaconMajorID, tmpIBeaconMajorID)
                                || !Utils.isByteValueEqual(mCfgIBeaconMinorID, tmpIBeaconMinorID)){
                            Log.e(TAG, "Write cfg ibeacon uuid failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgIBeaconUUID = tmpIBeaconUUID;
                    mCfgIBeaconMajorID = tmpIBeaconMajorID;
                    mCfgIBeaconMinorID = tmpIBeaconMinorID;
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get ibeacon uuid:" + byReadValue.toString());

                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_UID_DATA_UUID)) {
                    if (byReadValue.length < 16){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    byte[]tmpEddyNamespaceID = new byte[10];
                    byte[]tmpCfgEddySerialID = new byte[6];
                    System.arraycopy(byReadValue, 0, tmpEddyNamespaceID, 0, 10);
                    System.arraycopy(byReadValue, 10, tmpCfgEddySerialID, 0, 6);

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (!Utils.isByteValueEqual(mCfgEddyNamespaceID, tmpEddyNamespaceID)
                                || !Utils.isByteValueEqual(mCfgEddySerialID, tmpCfgEddySerialID)){

                            Log.e(TAG, "Write cfg eddy uid failed");

                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgEddyNamespaceID = tmpEddyNamespaceID;
                    mCfgEddySerialID = tmpCfgEddySerialID;
                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get uid uuid:" + byReadValue.toString());

                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_ADV_TYPE_UUID)) {
                    if (byReadValue.length < 1){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (mCfgAdvType != byReadValue[0]){
                            Log.e(TAG, "Write cfg adv type failed");

                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    mCfgAdvType = byReadValue[0];

                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get adv type:" + mCfgAdvType);

                }
                else if (cId.equals(Utils.CHR_BEACON_CFG_DEV_NAME_UUID)) {
                    if (byReadValue.length < 1){
                        Log.e(TAG, mMacAddress + ":onCharacteristicRead get data invalid");
                        mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                        return;
                    }
                    String strTemp = new String(byReadValue);
                    String strCfgName = mCfgDevName + "_" + mCfgDevSerialID;

                    if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck)
                    {
                        if (!strCfgName.equals(strTemp)){
                            Log.e(TAG, "Write cfg device name failed");
                            mReadErrorCharacterList.add(cId);
                        }
                    }

                    int splitIndex = strTemp.indexOf('_');
                    if (splitIndex != -1 && splitIndex > 0){
                        mCfgDevName = strTemp.subSequence(0, splitIndex).toString();
                        if (strTemp.length() > splitIndex+1) {
                            mCfgDevSerialID = strTemp.subSequence(splitIndex + 1, strTemp.length()).toString();
                        }
                    }else{
                        mCfgDevName = strTemp;
                    }

                    Log.v(TAG, mMacAddress + ":onCharacteristicRead get adv type:" + mCfgAdvType);

                }

                //read next characteristic
                if (mActionPendingStatus == eActionPendingStatus.ActionPendingInitReadCfg
                        || mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck){
                    sendReadNextBeaconInfoReq();
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            Log.v(TAG, "onCharacteristicWrite " + gatt.getDevice().getName()
                    + " write "
                    + characteristic.getUuid().toString()
                    + " -> "
                    + new String(characteristic.getValue()));

            //enable key notifycation
            UUID serviceUuid = characteristic.getService().getUuid();
            UUID charUuid = characteristic.getUuid();
            if (!serviceUuid.equals(Utils.SRV_BEACON_CUSTOM_UUID))
            {
                return;
            }

            if (mActionPendingStatus == eActionPendingStatus.ActionPendingWriteCfg) {
                sendWriteNextBeaconInfoReq();
            }else if (mActionPendingStatus == eActionPendingStatus.ActionPendingInitReadCfg)
            {
                //start read data after unlock complete
                if (charUuid.equals(Utils.CHR_BEACON_CFG_UNLOCK_UUID)) {
                    mNextReadUUIDIndex = 0;
                    sendReadNextBeaconInfoReq();
                }
            }
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //check if success
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, mMacAddress + ": onDescriptorWrite failed, status is " + status);
                mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                return;
            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            String strMac = gatt.getDevice().getAddress();
            if (!strMac.equals(mMacAddress)) {
                Log.e(TAG, ":get iTrack failed.");
                return;
            }

            UUID charUuid = characteristic.getUuid();
        };
    };

    //TODO 处理断开连接
    private void handleConnTimeout() {
        int nOldConnState = mBleConnStatus;
        if ((nOldConnState == BEACON_CONN_STATUS_CONNECTING)
                && mReConnNum < 1) {
            closeForQuickReconnect();

            mHandler.sendEmptyMessageDelayed(MSG_START_RECONNECT_DEVICE, 300);
        } else {
            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
        }
    }

    void handleConnSucc() {
        int nOldConnState = mBleConnStatus;
        mBleConnStatus = BEACON_CONN_STATUS_CONNECTED;
        sendConnChgBroadcast(nOldConnState, mBleConnStatus);

        mReConnNum = 0;

        mHandler.removeMessages(MSG_CONNECT_DEVICE_TIMEOUT);

        //notify conn success
        Message msg = mParentHandler.obtainMessage(BeaconPerpMgr.MSG_CONN_SUCCESS);
        msg.obj = mMacAddress;
        mParentHandler.sendMessage(msg);
    }


    private void startReadDeviceInfo(java.util.UUID characterID) {
        BluetoothGattCharacteristic characteristic = getCharacteristicByID(Utils.SRV_BEACON_CUSTOM_UUID,
                characterID);
        if (characteristic == null) {
            Log.e(TAG, ":startReadDeviceInfo getCharacteristicByID failed." + characterID);
            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
            return;
        }

        if (!mBleGatt.readCharacteristic(characteristic)) {
            Log.e(TAG, ":startReadDeviceInfo readCharacteristic failed:" + characterID);
            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
        }
        else
        {
            Log.v(TAG, "start read data success:" + characterID);
        }
    }

    private void startWriteDeviceInfo(java.util.UUID characterID) {
        BluetoothGattCharacteristic characteristic = getCharacteristicByID(Utils.SRV_BEACON_CUSTOM_UUID,
                characterID);
        if (characteristic == null) {
            Log.e(TAG, ":startWriteDeviceInfo getCharacteristicByID failed.");
            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
            return;
        }

        if (characterID.equals(Utils.CHR_BEACON_CFG_ADV_FLAG_UUID)) {
            byte []byTmpValue = new byte[1];
            byTmpValue[0] = mCfgAdvFlag;
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo advflag:" + mCfgAdvFlag);
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_LOCK_UUID)) {
            characteristic.setValue(mCfgNewLockCode);
            mPref.setSingleBeaconPassword(mMacAddress, mCfgNewPassword);
            Log.v(TAG, ":startWriteDeviceInfo lockcode:" + Utils.bytesToHexString(mCfgNewLockCode));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_UNLOCK_UUID)) {
            String strPassword = mPref.getSingleBeaconPassword(mMacAddress);
            setCfgLockPassword(strPassword);
            characteristic.setValue(mCfgUnlockCode);
            Log.v(TAG, ":startWriteDeviceInfo UnLockcode:" + Utils.bytesToHexString(mCfgUnlockCode));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_URI_DATA_UUID)) {
            characteristic.setValue(mCfgEddyUriData);
            Log.v(TAG, ":startWriteDeviceInfo URL Data:" + Utils.bytesToHexString(mCfgEddyUriData));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_TX_POWER_LVL_IDX_UUID)) {
            byte []byTmpValue = new byte[1];
            byTmpValue[0] = mCfgBeaconTxIdx;
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo URL Data:" + mCfgBeaconTxIdx);
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_TX_POWER_LVL_UUID)) {
            characteristic.setValue(mCfgBeaconTxPowerLvls);
            Log.v(TAG, ":startWriteDeviceInfo mCfgBeaconTxPowerLvls:" + Utils.bytesToHexString(mCfgBeaconTxPowerLvls));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_ADV_PERIOD_UUID)) {
            byte []byTmpValue = new byte[2];
            byTmpValue[1] = (byte)((mCfgBeaconAdvPeriod & 0xFF00) >> 8);
            byTmpValue[0] = (byte)(mCfgBeaconAdvPeriod & 0xFF);
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo mCfgBeaconAdvPeriod:" + mCfgBeaconAdvPeriod);
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_RESET_UUID)) {
            byte []byTmpValue = new byte[1];
            byTmpValue[0] = 1;
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo cfg reset value:" + byTmpValue[0]);
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID)) {
            byte []byTmpValue = new byte[16+4];
            System.arraycopy(mCfgIBeaconUUID, 0, byTmpValue, 0, 16);
            System.arraycopy(mCfgIBeaconMajorID, 0, byTmpValue, 16, 2);
            System.arraycopy(mCfgIBeaconMinorID, 0, byTmpValue, 18, 2);
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo cfg mCfgIBeaconUUID:" + Utils.bytesToHexString(byTmpValue));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_UID_DATA_UUID)) {
            byte []byTmpValue = new byte[16];
            System.arraycopy(mCfgEddyNamespaceID, 0, byTmpValue, 0, 10);
            System.arraycopy(mCfgEddySerialID, 0, byTmpValue, 10, 6);
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo cfg mCfgEddystoneUID:" + Utils.bytesToHexString(byTmpValue));
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_ADV_TYPE_UUID)) {
            byte []byTmpValue = new byte[1];
            byTmpValue[0] = mCfgAdvType;
            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo cfg mCfgAdvType:" + mCfgAdvType);
        }
        else if (characterID.equals(Utils.CHR_BEACON_CFG_DEV_NAME_UUID)) {
            String strWriteData = mCfgDevName + "_" + mCfgDevSerialID;
            char []csDevName = strWriteData.toCharArray();
            byte []byTmpValue = new byte[strWriteData.length()];
            for (int i = 0; i < byTmpValue.length; i++){
                byTmpValue[i] = (byte)csDevName[i];
            }

            characteristic.setValue(byTmpValue);
            Log.v(TAG, ":startWriteDeviceInfo cfg mCfgDevName:" + strWriteData);
        }

        //start read status
        if (!mBleGatt.writeCharacteristic(characteristic))
        {
            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
            Log.e(TAG, "start write data failed");
        }
        else
        {
            Log.v(TAG, "start write data success:" + characterID);
        }
    }

    private void startConnDev() {
        mHandler.removeMessages(MSG_START_CONNECT_DEVICE);
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mMacAddress);
        int nOldBleConnStatus = mBleConnStatus;
        mBleConnStatus = BEACON_CONN_STATUS_CONNECTING;
        mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;
        mReConnNum = 0;

        mBleGatt = device.connectGatt(mContext, false, mGattCallback);

        mHandler.removeMessages(MSG_CONNECT_DEVICE_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(MSG_CONNECT_DEVICE_TIMEOUT, MAX_CONN_TIME);

        sendConnChgBroadcast(nOldBleConnStatus, mBleConnStatus);
    }

    private void startReConnectDev() {
        int nOldBleConnStatus = mBleConnStatus;
        mBleConnStatus = BEACON_CONN_STATUS_CONNECTING;
        mReConnNum++;

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mMacAddress);
        mBleConnStatus = BEACON_CONN_STATUS_CONNECTING;
        mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;

        mBleGatt = device.connectGatt(mContext, false, mGattCallback);

        //extend timeout
        extendConnTimeout();

        sendConnChgBroadcast(nOldBleConnStatus, mBleConnStatus);
    }

    public void startConnect() {
        if (mBleGatt != null) {
            mBleGatt.close();
            mBleGatt = null;
        }

        mHandler.sendEmptyMessageDelayed(MSG_START_CONNECT_DEVICE, 300);
    }

    private void extendConnTimeout()
    {
        mHandler.removeMessages(MSG_CONNECT_DEVICE_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(MSG_CONNECT_DEVICE_TIMEOUT, MAX_CONN_TIME);

        Message msg = mParentHandler.obtainMessage(BeaconPerpMgr.MSG_EXTEND_CONN_TIMEOUT);
        msg.obj = mMacAddress;
        mParentHandler.sendMessage(msg);
    }


    public boolean isConnected() {
        return mBleConnStatus == BEACON_CONN_STATUS_CONNECTED;
    }

    public boolean isDisconnected() {
        return mBleConnStatus == BEACON_CONN_STATUS_DISCONNECTED;
    }

    public int getConnStatus() {
        return mBleConnStatus;
    }

    public String getAdvBattery()
    {
        if (mAdvBatteryLevel == INVALID_INT_VALUE)
        {
            return "N/A";
        }

        return String.format("%dmV", mAdvBatteryLevel);
    }

    public String getAdvPower()
    {
        if (mAdvTxPowerLevel == INVALID_INT_VALUE)
        {
            return "N/A";
        }

        return String.format("%d", mAdvTxPowerLevel);
    }

    public int getAdvRssiInt()
    {
        if (mAdvRssiResult == INVALID_INT_VALUE)
        {
            return 0;
        }

        return mAdvRssiResult;
    }

    public String getAdvRssi()
    {
        if (mAdvRssiResult == INVALID_INT_VALUE)
        {
            return "N/A";
        }

        return String.format("%d", mAdvRssiResult);
    }

    public String getAdvConnEnableString()
    {
        if (mConnEnable) {
            return "Yes";
        }else{
            return "NO";
        }
    }

    public boolean getAdvConnEnable()
    {
        return mConnEnable;
    }

    public String getRtt()
    {
        if (mAdvTLMRttTick != 0){
            return String.format("%dms", mAdvTLMRttTick);
        }else {
            return String.format("%dms", mAdvRttTick);
        }
    }

    public String getMajorID()
    {
        if (mAdvIBeaconMajorID == null){
            return "N/A";
        }

        return String.format("%s", mAdvIBeaconMajorID);
    }

    public String getMacAddress()
    {
        return mMacAddress;
    }

    public String getMacAddrEx()
    {
        if (mMacAddress == null){
            return "N/A";
        }

        String strMac = mMacAddress.toLowerCase();
        return strMac;
    }

    public int getCfgBeaconAdvType(){
        return mCfgAdvType;
    }

    public int getCfgBeaconAdvFlag(){
        return mCfgAdvFlag;
    }

    public boolean isDeviceLocked()
    {
        return mCfgBeaconLockstatus;
    }

    public void setCfgBeaconAdvFlag(int nAdvFlag){
        if (mCfgAdvFlag != nAdvFlag) {
            mCfgAdvFlag = (byte) nAdvFlag;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_ADV_FLAG_UUID);
        }
    }

    public void setCfgBeaconAdvType(int nCfgAdvType){
        if (mCfgAdvType != nCfgAdvType) {
            mCfgAdvType = (byte) nCfgAdvType;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_ADV_TYPE_UUID);
        }
    }

    public void addCfgType2WriteList(java.util.UUID uuid){
        boolean bFound =false;
        for (java.util.UUID existUUID : mWriteCharacterList){
            if (existUUID.equals(uuid)){
                bFound = true;
            }
        }
        if (!bFound) {
            mWriteCharacterList.add(uuid);
        }
    }

    public void rmvCfgType2WriteList(java.util.UUID uuid){
        mWriteCharacterList.remove(uuid);
    }

    public String getCfgTxPowrLvls(){
        String strTxPowrLvls = "";
        for (int i = 0; i < 4; i++){
            int nTxPower = mCfgBeaconTxPowerLvls[i];
            strTxPowrLvls += String.valueOf(nTxPower);
            strTxPowrLvls += "; ";
        }

        return strTxPowrLvls;
    }

    public boolean setCfgBeaconTxPowerLvls(String strPwrLvls){
        strPwrLvls = strPwrLvls.trim();
        String []pwrLvls = strPwrLvls.split(";");
        if (pwrLvls.length != 4){
            return false;
        }

        int[] byPwrLvls = new int[4];
        for (int i = 0; i < 4; i++){
            pwrLvls[i] = pwrLvls[i].trim();
            try {
                byPwrLvls[i] = Integer.valueOf(pwrLvls[i]);
            }catch (NumberFormatException numberExcp){
                return false;
            }
            if (byPwrLvls[i] > 5 || byPwrLvls[i] < -20){
                return false;
            }
        }

        boolean bDataChg = false;
        for (int i = 0; i < pwrLvls.length; i++) {
            if (mCfgBeaconTxPowerLvls[i] != byPwrLvls[i]) {
                bDataChg = true;
                mCfgBeaconTxPowerLvls[i] = (byte)byPwrLvls[i];
            }
        }
        if (bDataChg){
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_TX_POWER_LVL_UUID);
        }

        return true;
    }

    public String getCfgBeaconTxPower(int i){
        if (i > 4){
            return null;
        }

        int nTxPower = mCfgBeaconTxPowerLvls[i];
        return String.valueOf(nTxPower);
    }

    public int getCfgBeaconTxPowerIdx(){
        return mCfgBeaconTxIdx;
    }

    public boolean setCfgBeaconTxPowerIdx(int nIndex){
        if (nIndex > 3){
            return false;
        }
        if (mCfgBeaconTxIdx != nIndex){
            mCfgBeaconTxIdx = (byte)nIndex;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_TX_POWER_LVL_IDX_UUID);
        }

        return true;
    }

    public String getCfgBeaconSerialID(){
        return mCfgDevSerialID;
    }

    public boolean setCfgBeaconSerialID(String strSerialID){
        try {
            int tempPeriod = Integer.valueOf(strSerialID);
            if (tempPeriod < 0 || tempPeriod > 99999){
                return false;
            }

            if (!strSerialID.equals(mCfgDevSerialID)){
                mCfgDevSerialID = strSerialID;
                addCfgType2WriteList(Utils.CHR_BEACON_CFG_DEV_NAME_UUID);
            }
            return true;
        }catch (NumberFormatException error){
            return false;
        }
    }

    public void clearForWriteCfg(){
        mWriteCharacterList.clear();
        mReadErrorCharacterList.clear();
        mNextWriteUUIDIndex = 0;
        mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;
    }

    private String mCfgOldLockPwd = "";
    private boolean setCfgLockPassword(String strPassword){
        if (strPassword.length() != 6){
            return false;
        }

        char[] chPassword = strPassword.toCharArray();

        for (int i = 16 - 6, k = 0; k < 6; k++, i++) {
            mCfgUnlockCode[i] = (byte)chPassword[k];
        }
        mCfgOldLockPwd = strPassword;

        return true;
    }

    private String mCfgNewPassword;
    public boolean setCfgNewLockPassword(String strPassword){
        if (strPassword.length() != 6){
            return false;
        }

        char[] chPassword = strPassword.toCharArray();
        for (int i = 16 - 6, k = 0; k < 6; k++, i++) {
            mCfgNewLockCode[i] = (byte)chPassword[k];
        }

        mCfgNewPassword = strPassword;

        addCfgType2WriteList(Utils.CHR_BEACON_CFG_LOCK_UUID);

        //after change password, the app should use new password to unlock the device again
        addCfgType2WriteList(Utils.CHR_BEACON_CFG_UNLOCK_UUID);

        return true;
    }

    public String getCfgBeaconName(){
        return mCfgDevName;
    }

    public boolean setCfgBeaconName(String strName){
        if (strName.equals("") || strName.length() > 11){
            return false;
        }

        if (!mCfgDevName.equals(strName)){
            mCfgDevName = strName;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_DEV_NAME_UUID);
        }
        return true;
    }

    public String getAdvMinorID()
    {
        if (mAdvIBeaconMinorID == null){
            return "N/A";
        }

        return String.format("%s", mAdvIBeaconMinorID);
    }

    public String getAdvTemputure()
    {
        if (mAdvTemputure == INVALID_INT_VALUE){
            return "N/A";
        }

        String strTemp = String.format("%.1f℃", mAdvTemputure);

        return strTemp;
    }

    public int getAdvBeaconType()
    {
        return mAdvBeaconType;
    }

    public String getAdvIBeaconUUID()
    {
        if (mAdvIBeaconUUID == null){
            return "N/A";
        }

        return String.format("%s", mAdvIBeaconUUID);
    }

    public String getAdvEddystoneUID()
    {
        return String.format("%s-%s", mAdvEddystoneNamesapceID, mAdvEddystoneSerialID);
    }

    public String getAdvEddystoneUrl()
    {
        return String.format("%s", mAdvEddystoneUrl);
    }


    private BluetoothGattCharacteristic getCharacteristicByID(java.util.UUID srvUUID, java.util.UUID charaID) {
        if (mBleGatt == null) {
            Log.e(TAG, ":mBleGatt is null");
            return null;
        }
        BluetoothGattService service = mBleGatt.getService(srvUUID);
        if (service == null) {
            Log.e(TAG, ":getCharacteristicByID get services failed." + srvUUID);
            return null;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(charaID);
        if (characteristic == null) {
            Log.e(TAG, ":getCharacteristicByID get characteristic failed." + charaID);
            return null;
        }

        return characteristic;
    }

    public void startUploadCfgParam2Device(String strLockCode)
    {
        if (mWriteCharacterList.size() == 0){
            sendAppChgBroadcast(BEACON_NTF_MO_CFG_CHANGE, 0);
            return;
        }

        mNextWriteUUIDIndex = 0;
        mNextReadUUIDIndex = 0;
        mReadCharacterList.clear();

        mActionPendingStatus = eActionPendingStatus.ActionPendingWriteCfg;
        sendWriteNextBeaconInfoReq();
    }

    public void closeConnection() {
        if (mBleGatt != null) {
            Log.e(TAG, "closeConnection:disconnect and close connection for device.");
            mBleGatt.disconnect();

            mBleGatt.close();
            mBleGatt = null;
        }

        mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;
        int nOldConnStatus = mBleConnStatus;
        mBleConnStatus = BEACON_CONN_STATUS_DISCONNECTED;
        sendConnChgBroadcast(nOldConnStatus, mBleConnStatus);
        mKeyNotify = 0;
    }

    public void closeForQuickReconnect() {
        Log.e(TAG, "closeForQuickReconnect: close connection for device.");
        if (mBleGatt != null){
            mBleGatt.close();
            mBleGatt = null;
        }

        mActionPendingStatus = eActionPendingStatus.ActionPendingNULL;

        //int nOldConnStatus = mBleConnStatus;
        mBleConnStatus = BEACON_CONN_STATUS_DISCONNECTED;
        //sendConnChgBroadcast(nOldConnStatus, mBleConnStatus);

        mKeyNotify = 0;
    }

    void sendConnChgBroadcast(int nOldConnStatus, int nNewConnStatus) {
        final Intent intent = new Intent(BEACON_CONN_STATUS_CHG);
        intent.putExtra(BEACON_MAC_ADDRESS, mMacAddress);
        intent.putExtra(BEACON_OLD_CONN_STATUS, nOldConnStatus);
        intent.putExtra(BEACON_NEW_CONN_STATUS, nNewConnStatus);
        mContext.sendBroadcast(intent);
    }

    public void sendAppChgBroadcast(int nNtfMsgType, int nExtValue) {
        final Intent intent = new Intent(BEACON_APP_CHG);
        intent.putExtra(BEACON_MAC_ADDRESS, mMacAddress);
        intent.putExtra(BEACON_APP_VALUE, nNtfMsgType);
        intent.putExtra(BEACON_EXT_VALUE, nExtValue);

        mContext.sendBroadcast(intent);
    }

    public void updateAdvName(String strDevName){
        mAdvName = strDevName;
    }

    public void updateConnable(boolean bConnEnable)
    {
        mConnEnable = bConnEnable;
    }

    public void updateAdvRSSI(int nNewRssi, long currTick) {
        if (mAdvRssiResult == 0) {
            mAdvRssiResult = nNewRssi;
        } else {
            mAdvRssiResult = (int)((float)mAdvRssiResult * 0.3 + (float)nNewRssi*0.7);
        }

        //get rtt value
        long nLapseTick = 1000;
        if (mAdvLastUpdateTick != 0){
            nLapseTick = currTick - mAdvLastUpdateTick;
        }
        mAdvLastUpdateTick = currTick;
        mAdvRttTick = (int)((float)mAdvRttTick * 0.3) + (int)((float)nLapseTick * 0.7);
    }

    private String fromByte2String(byte[] byData){
        String strTemp = "0X";
        if (byData == null){
            return "";
        }

        return Utils.bytesToHexString(byData);
    }

    public String getCfgIBeaconUUID(){
        return fromByte2String(mCfgIBeaconUUID);
    }

    public boolean setCfgIBeaconUUID(String strIBeaconUUID){
        if (strIBeaconUUID.length() != 32){
            return false;
        }

        byte[] byTemp = Utils.hexStringToBytes(strIBeaconUUID);
        if (byTemp == null){
            return false;
        }
        if (byTemp.length != mCfgIBeaconUUID.length){
            return false;
        }
        if (!Utils.isByteValueEqual(mCfgIBeaconUUID, byTemp)){
            mCfgIBeaconUUID = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID);
        }
        return true;
    }

    public boolean setCfgMajorID(String strMajorID){
        if (strMajorID.length() != 4){
            return false;
        }
        byte[] byTemp = Utils.hexStringToBytes(strMajorID);
        if (byTemp == null){
            return false;
        }
        if (byTemp.length != mCfgIBeaconMajorID.length){
            return false;
        }

        if (!Utils.isByteValueEqual(mCfgIBeaconMajorID, byTemp)){
            mCfgIBeaconMajorID = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID);
        }
        return true;
    }

    public boolean setCfgMinorID(String strMinorID){
        if (strMinorID.length() != 4){
            return false;
        }

        byte[] byTemp = Utils.hexStringToBytes(strMinorID);
        if (byTemp == null){
            return false;
        }
        if (byTemp.length != mCfgIBeaconMajorID.length){
            return false;
        }

        if (!Utils.isByteValueEqual(mCfgIBeaconMinorID, byTemp)){
            mCfgIBeaconMinorID = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_IBEACON_DATA_UUID);
        }

        return true;
    }

    public boolean setCfgEddyNamespaceID(String strNameSpaceID){
        if (strNameSpaceID.length() != 20){
            return false;
        }

        byte[] byTemp = Utils.hexStringToBytes(strNameSpaceID);
        if (byTemp == null){
            return false;
        }
        if (byTemp.length != mCfgEddyNamespaceID.length){
            return false;
        }

        if (!Utils.isByteValueEqual(mCfgEddyNamespaceID, byTemp)){
            mCfgEddyNamespaceID = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_UID_DATA_UUID);
        }

        return true;
    }

    public boolean setCfgEddySerialID(String strSerialID){
        if (strSerialID.length() != 12){
            return false;
        }

        byte[] byTemp = Utils.hexStringToBytes(strSerialID);
        if (byTemp == null){
            return false;
        }
        if (byTemp.length != mCfgEddySerialID.length){
            return false;
        }

        if ( !Utils.isByteValueEqual(mCfgEddySerialID, byTemp)){
            mCfgEddySerialID = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_UID_DATA_UUID);
        }

        return true;
    }

    public int setCfgEddyUrl(String strUrl){
        char []chEddyUrl = strUrl.toCharArray();
        char []chEddyEnc = new char[EDDYSTONE_MAX_URL_ENCODE_LEN];

        int nLenght = Utils.EddystoneBeacon_encodeURL(chEddyUrl, chEddyEnc);
        if (nLenght <= 0){
            return nLenght;
        }

        byte []byTemp = new byte[nLenght];
        for (int i = 0; i < nLenght; i++){
            byTemp[i] = (byte)(chEddyEnc[i]);
        }

        if ( !Utils.isByteValueEqual(mCfgEddyUriData, byTemp)){
            mCfgEddyUriData = byTemp;
            addCfgType2WriteList(Utils.CHR_BEACON_CFG_URI_DATA_UUID);
        }

        return nLenght;
    }

    public int getCfgAdvPeriod(){
        return mCfgBeaconAdvPeriod;
    }



    public boolean setCfgAdvPeriod(String strAdvPeriod){
        try {
            int tempPeriod = Integer.valueOf(strAdvPeriod);
            if ((tempPeriod < 100 || tempPeriod > 10000) && tempPeriod != 0){
                return false;
            }

            if (tempPeriod != mCfgBeaconAdvPeriod){
                mCfgBeaconAdvPeriod = tempPeriod;
                addCfgType2WriteList(Utils.CHR_BEACON_CFG_ADV_PERIOD_UUID);
            }
            return true;
        }catch (NumberFormatException error){
            return false;
        }
    }

    public String getCfgMajorID(){
        return fromByte2String(mCfgIBeaconMajorID);
    }

    public String getCfgMinorID(){
        return fromByte2String(mCfgIBeaconMinorID);
    }

    public String getCfgEddyUrl(){

        String strUrl = Utils.EddystoneBeacon_DecodeURL(mCfgEddyUriData, mCfgEddyUriData.length);
        if (strUrl == null) {
            return "";
        }else{
            return strUrl;
        }
    }

    public String getCfgEddyNamespaceID(){
        return fromByte2String(mCfgEddyNamespaceID);
    }

    public String getCfgEddySerialID(){
        return fromByte2String(mCfgEddySerialID);
    }

    public void sendReadNextBeaconInfoReq()
    {
        if (mNextReadUUIDIndex < mReadCharacterList.size())
        {
            java.util.UUID uuid = mReadCharacterList.get(mNextReadUUIDIndex);
            if (uuid != null) {

                //check if characteristic is exist
                BluetoothGattCharacteristic characteristic = getCharacteristicByID(Utils.SRV_BEACON_CUSTOM_UUID,
                        uuid);
                if (characteristic == null) {
                    Log.e(TAG, ":sendReadNextBeaconInfoReq getCharacteristicByID failed." + uuid);
                    sendAppChgBroadcast(BEACON_NTF_DEV_UNKNOWN, 0);
                    mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                    return;
                }
                Log.v(TAG, "sendReadNextBeaconInfoReq start read next uuid:" + uuid);

                mNextReadUUIDIndex++;
                Message msg = mHandler.obtainMessage(MSG_BEACON_START_READ_PARA);
                msg.obj = uuid;
                mHandler.sendMessage(msg);

                if (mActionPendingStatus == eActionPendingStatus.ActionPendingIReadCheck) {
                    int nReadPercent = (int) (100 * ((float) mNextReadUUIDIndex / mReadCharacterList.size()));
                    Log.v(TAG, "Read Config percent:" + nReadPercent);
                    sendAppChgBroadcast(BEACON_NTF_RD_CFG_PROGRESS, nReadPercent);
                }
            }
        } else {
            Log.v(TAG, "sendReadNextBeaconInfoReq red uuid complete");
            mHandler.sendEmptyMessageDelayed(MSG_READ_CHAR_COMPLETE, 300);
        }
    }

    public void sendWriteNextBeaconInfoReq()
    {
            do {
                if (mNextWriteUUIDIndex < mWriteCharacterList.size()) {
                    java.util.UUID uuid = mWriteCharacterList.get(mNextWriteUUIDIndex);
                    if (uuid != null) {
                        //check if characteristic is exist
                        BluetoothGattCharacteristic characteristic = getCharacteristicByID(Utils.SRV_BEACON_CUSTOM_UUID,
                                uuid);
                        if (characteristic == null) {
                            Log.e(TAG, ":sendWriteNextBeaconInfoReq getCharacteristicByID failed." + uuid);
                            sendAppChgBroadcast(BEACON_NTF_DEV_UNKNOWN, 0);
                            mHandler.sendEmptyMessage(MSG_CLOSE_CONNECTION);
                            return;
                        }

                        //lock code can not be read, ignore
                        if (!uuid.equals(Utils.CHR_BEACON_CFG_LOCK_UUID)
                                 && !uuid.equals(Utils.CHR_BEACON_CFG_UNLOCK_UUID)) {
                            mReadCharacterList.add(uuid);
                        }

                        mNextWriteUUIDIndex++;
                        Message msg = mHandler.obtainMessage(MSG_BEACON_START_WRITE_PARA);
                        msg.obj = uuid;
                        mHandler.sendMessage(msg);

                        if (mActionPendingStatus == eActionPendingStatus.ActionPendingWriteCfg) {
                            int nWritePercent = (int) (100 * ((float) mNextWriteUUIDIndex / mWriteCharacterList.size()));
                            Log.v(TAG, "Write Config percent:" + nWritePercent);
                            sendAppChgBroadcast(BEACON_NTF_WR_CFG_PROGRESS, nWritePercent);
                        }
                    }
                    return;
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_WRITE_CHAR_COMPLETE, 300);
                    return;
                }
            }while(true);
    }

}
