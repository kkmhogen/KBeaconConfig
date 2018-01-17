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

package com.beacon.kbeacon.KBeaconConfigTools;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener{
	private final static String TAG = "Beacon.ScanAct";//DeviceScanActivity.class.getSimpleName();


    private final static int MSG_START_SCAN = 201;
    private final static int MSG_SCAN_TIMEOUT = 202;
    private final static int MSG_CHK_TIMER_MY = 203;
    private final static int MSG_REFERASH_VIEW = 204;
    private final static int MSG_REFERASH_ONE_ITEM_VIEW = 205;
    private final static int MSR_START_SORT_AGAIN = 206;
    private final static int MSG_FIND_NEW_DEVICE = 208;



    private final static int DEV_SORT_PERIOD = 2000;

    private final static String CFG_MODE_BEACON_NAME = "KBeacon";


    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**读写BLE终端*/
    private BeaconPerpMgr mBLESrvMgr;
    private ListView mListView;
    private EditText mEditFltDevName;
    private LeDeviceListAdapter mListViewAdapter;
    private boolean mScanning;
    public Handler mHandler;
    private SharePreferenceMgr mPrefMgr = null;
    private ImageButton mImgButtonRssiSort;
    private Button mFilterButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String mFilterName = "";

    // Stops scanning after 10 seconds.
    private static final long SCAN_POST_DELAY = 5;
    private static final long SCAN_PERIOD = 120* 1000;
    private static final long CHK_TIMER_PERIOD = 4 * 1010;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BeaconPerperal.BEACON_CONN_STATUS_CHG.equals(action)) {
                updateListView();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MsgHandler();

        setContentView(R.layout.main_activity);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //开启蓝牙
        mBluetoothAdapter.enable();

        mBLESrvMgr = BeaconPerpMgr.shareInstance(this);
        if (mBLESrvMgr == null) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }

        mPrefMgr = SharePreferenceMgr.shareInstance(getApplication());


        mHandler.sendEmptyMessageDelayed(MSG_CHK_TIMER_MY, CHK_TIMER_PERIOD);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mNPhoneCallback = new NPhoneScancallback();
        }

        mListView = (ListView) findViewById(R.id.listview);
        mListViewAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mListViewAdapter);
        mListView.setOnItemClickListener(this);

        mImgButtonRssiSort = (ImageButton)findViewById(R.id.imageButtonRssiSort);
        mImgButtonRssiSort.setOnClickListener(this);

        mEditFltDevName = (EditText)findViewById(R.id.editTextFltDevName);
        mEditFltDevName.addTextChangedListener(new EditChangedListener());

        mFilterButton = (Button)findViewById(R.id.buttonRssiFilter);
        mFilterButton.setOnClickListener(this);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        //设置刷新时动画的颜色，可以设置4个
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        swipeRefreshLayout.setRefreshing(false);

                        reStartScan();
                    }
                }, 500);
            }
        });
    }

    private boolean mEnalbeRSSISort = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.imageButtonRssiSort:

                if (mEnalbeRSSISort)
                {
                    Drawable rssiDisPic = ContextCompat.getDrawable(this, R.drawable.rssi_inc_disable);
                    mImgButtonRssiSort.setImageDrawable(rssiDisPic);
                    mEnalbeRSSISort = false;
                }
                else {
                    Drawable rssiSortPic = ContextCompat.getDrawable(this, R.drawable.rssi_inc_enable);
                    mImgButtonRssiSort.setImageDrawable(rssiSortPic);
                    mEnalbeRSSISort = true;
                    mHandler.sendEmptyMessageDelayed(MSR_START_SORT_AGAIN, DEV_SORT_PERIOD);
                }

                break;

            case R.id.buttonRssiFilter:
            {
                filterRssiDlg();
                break;
            }
        }
    }

    void filterRssiDlg()
    {
        //view
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.activity_rssi_filter, null);
        final EditText txtRssiFrom = (EditText) textEntryView.findViewById(R.id.txtFromRssi);
        final EditText txtRssiTo = (EditText)textEntryView.findViewById(R.id.txtToRssi);

        //create dlg
        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceScanActivity.this);
        builder.setTitle(getString(R.string.RSSI_FILTER_BUTTON));
        builder.setView(textEntryView);
        txtRssiFrom.setText(String.valueOf(mPrefMgr.getMinRssiFilter()));
        txtRssiTo.setText(String.valueOf(mPrefMgr.getMaxRssiFilter()));
        builder.setNegativeButton(R.string.Dialog_Cancel, null);
        builder.setPositiveButton(R.string.Dialog_OK, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nMinRssiValue = -100, nMaxRssiValue = 0;
                try {
                    nMinRssiValue = Integer.valueOf(txtRssiFrom.getText().toString());
                    nMaxRssiValue = Integer.valueOf(txtRssiTo.getText().toString());
                }catch (NumberFormatException excpt)
                {
                    toastShow(getString(R.string.input_rssi_error));
                    return;
                }
                if (nMinRssiValue > nMaxRssiValue){
                    toastShow(getString(R.string.min_large_then_max_error));
                    return;
                }

                if (nMinRssiValue < -100 || nMaxRssiValue > 10)
                {
                    toastShow(getString(R.string.input_rssi_error));
                    return;
                }

                mPrefMgr.saveMinRssiFilter(nMinRssiValue);
                mPrefMgr.saveMaxRssiFilter(nMaxRssiValue);
                alertDialog.dismiss();
                mBLESrvMgr.closeAllConnection();
                updateListView();

                //mHandler.sendEmptyMessage(MSG_RSSI_FILTER_CHANGE);
            }
        });
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BeaconPerperal blePerp = getBlePerp(position);
        if (blePerp == null)
            return;

        if (!blePerp.getAdvConnEnable())
        {
            toastShow(getString(R.string.KBEACON_ADV_NO_CONNECTABLE));
            return;
        }

        stopScan();

        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(BeaconPerperal.BEACON_MAC_ADDRESS, blePerp.getMacAddress());
        startActivity(intent);

        Log.e(TAG, "click id:" + id );
    }

    public class MsgHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //start pair scan
                case MSG_START_SCAN: {
                    handleStartScan();
                    break;
                }

                case MSG_SCAN_TIMEOUT: {
                    handleStopScan();
                    break;
                }

                case MSG_FIND_NEW_DEVICE:{
                    ScanResult rslt = (ScanResult)msg.obj;
                    if (rslt != null) {
                        handleBleScanRslt(rslt);
                    }
                    break;
                }

                case MSG_REFERASH_VIEW: {
                    mHandler.removeMessages(MSG_REFERASH_VIEW);
                    updateListView();
                    break;
                }

                case MSR_START_SORT_AGAIN:{
                    mHandler.removeMessages(MSR_START_SORT_AGAIN);

                    if (mBLESrvMgr.sortDevByRSSI()){
                        updateListView();
                    }

                    if (mEnalbeRSSISort) {
                        mHandler.sendEmptyMessageDelayed(MSR_START_SORT_AGAIN, DEV_SORT_PERIOD);
                    }
                }

                case MSG_REFERASH_ONE_ITEM_VIEW:
                {
                    int position = msg.arg1;

                    if (mListView != null) {
                        View view = mListView.getChildAt(position - mListView.getFirstVisiblePosition());
                        if (view != null) {
                            mListViewAdapter.getView(position, view, mListView);
                        }
                    }
                    break;
                }

                case MSG_CHK_TIMER_MY:{
                    handlePeriodChk();
                    mHandler.sendEmptyMessageDelayed(MSG_CHK_TIMER_MY, CHK_TIMER_PERIOD);
                    break;
                }

                default:
                    break;
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BeaconPerperal.BEACON_CONN_STATUS_CHG);

        return intentFilter;
    }

    private void reStartScan(){
        mBLESrvMgr.closeAllConnection();

        stopScan();

        startScan();
    }


    @Override
    protected void onResume() {
        super.onResume();

        makeGattUpdateIntentFilter();

        if (!mBluetoothAdapter.isEnabled()){
            Toast.makeText(DeviceScanActivity.this,
                    R.string.ble_function_disable,
                    Toast.LENGTH_SHORT).show();
        }else {
            stopScan();

            startScan();
        }

        updateListView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopScan();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESrvMgr.closeAllConnection();
    }


    private void showSettingView()
    {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void stopScan()
    {
        mHandler.removeMessages(MSG_SCAN_TIMEOUT);

        try {
            if (mScanning) {
                mScanning = false;
                BluetoothLeScanner scaner = mBluetoothAdapter.getBluetoothLeScanner();
                scaner.stopScan(mNPhoneCallback);

                Log.e(TAG, "now stop scan success fully");
            }
        } catch (RuntimeException excp) {
            Log.e(TAG, "start scan error" + excp.getCause());
        }

    }

    private void handlePeriodChk(){
        long currTick = System.currentTimeMillis();
    }

    private void handleStartScan(){
        try {
            if (mScanning) {
                Log.e(TAG, "current is scan, now start scan again");
                mScanning = false;
                stopScan();
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
                //start scan
                ScanSettings.Builder setsBuild;
                setsBuild = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                BluetoothLeScanner scaner = mBluetoothAdapter.getBluetoothLeScanner();
                scaner.startScan(null, setsBuild.build(), mNPhoneCallback);
            }

            mScanning = true;
            Log.e(TAG, "now start scan success fully");


        }catch (RuntimeException excp)
        {
            Log.e(TAG, "start scan error" + excp.getCause());
        }
    }

    NPhoneScancallback mNPhoneCallback;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class NPhoneScancallback extends ScanCallback
    {
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null) {
                onDeviceFound(result);
            }
        }

        public void onBatchScanResults(List< android.bluetooth.le.ScanResult > results) {
            if (results.size() > 0){
                for(ScanResult rslt: results) {
                    onScanResult(10, rslt);
                }
            }else{
                Log.e(TAG, "Start N scan found 0 result");
            }
        }

        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Start N scan failed：" + errorCode);
            stopScan();
        }
    }

    public void handleStopScan(){
        stopScan();
    }

    public void startScan() {

        mListViewAdapter.notifyDataSetChanged();

        mHandler.sendEmptyMessageDelayed(MSG_START_SCAN, 500);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback;



    public BeaconPerperal getBlePerp(int nIndex){
        return mBLESrvMgr.getTrackByMac(nIndex);
    }

    public int getPositionByMac(String strMmacAddr){
        return mBLESrvMgr.getPositionByMac(strMmacAddr);
    }

    private static int APPLE_MANUFACTURE_ID = 0x004C;



    public void onDeviceFound(final ScanResult rslt)
    {
        //filter rssi
        int rssi = rslt.getRssi();
        if (rssi < mPrefMgr.getMinRssiFilter() || rssi > mPrefMgr.getMaxRssiFilter()){
            return;
        }

        //filter name
        if (mFilterName != null && !mFilterName.equals("")){
            String strDevName = rslt.getDevice().getName();
            if (strDevName != null) {
                strDevName = strDevName.toLowerCase();
                if (!strDevName.contains(mFilterName)) {
                    return;
                }
            }
        }

        Message msg = mHandler.obtainMessage(MSG_FIND_NEW_DEVICE);
        msg.obj = rslt;
        mHandler.sendMessage(msg);
    }


    public void handleBleScanRslt(final ScanResult rslt)
    {
        String strMinor = null, strMajor = null, strUUID = null;
        String strUidNamesapceID = null, strUidSerialID = null, strUrl = null;
        int nAdvCount = 0;
        int nBatteryLevel = BeaconPerperal.INVALID_INT_VALUE;
        float fTemputure = 0;

        int nBeaconPower = 0;

        BluetoothDevice device = rslt.getDevice();
        String strMacAddress = device.getAddress();
        int rssi = rslt.getRssi();


        ScanRecord record = rslt.getScanRecord();
        if (record == null){
            return;
        }
        String strDevName = record.getDeviceName();

        byte[] advData = record.getBytes();

        //get adv type
        int nAdvType = record.getAdvertiseFlags();

        //get beacon type
        int beaconType = BeaconPerperal.BEACON_TYPE_INVALID;
        if (record.getManufacturerSpecificData() != null) {
            byte[] iBeaconData = record.getManufacturerSpecificData(APPLE_MANUFACTURE_ID);
            if (iBeaconData != null) {
                beaconType = BeaconPerperal.BEACON_TYPE_IBEACON;
                //check if data length is right
                if (iBeaconData[1] < 0x15) {
                    return;
                }

                //get uuid
                strUUID = "0x";
                for (int i = 2; i < 18; i++) {
                    strUUID = strUUID + String.format("%02x", iBeaconData[i]);
                }

                //get major id
                strMajor = String.format("0x%02x%02x", iBeaconData[18], iBeaconData[19]);

                //get minor id
                strMinor = String.format("0x%02x%02x", iBeaconData[20], iBeaconData[21]);

                nBeaconPower = iBeaconData[22];
            }
        }

        if (record.getServiceData() != null)
        {
            byte[] srvData = record.getServiceData(Utils.PARCE_UUID_EDDYSTONE);
            if (srvData != null) {
                int nSrvIndex = 0;
                beaconType = srvData[nSrvIndex++];
                if (beaconType == BeaconPerperal.BEACON_TYPE_EDDY_TLM) {
                    if (srvData.length < BeaconPerperal.MIN_EDDY_TLM_LEN) {
                        return;
                    }

                    //skip version
                    nSrvIndex++;

                    //battery
                    nBatteryLevel = (srvData[nSrvIndex++] & 0xFF);
                    nBatteryLevel = (nBatteryLevel << 8);
                    nBatteryLevel += (srvData[nSrvIndex++] & 0xFF);

                    //temputure
                    int nTempPointLeft = (srvData[nSrvIndex++] & 0xFF);
                    int nTempPointRight = (srvData[nSrvIndex++] & 0xFF);
                    String strTemp = String.format("%d.%d", nTempPointLeft, nTempPointRight);
                    fTemputure = Float.valueOf(strTemp);

                    //adv count
                    nAdvCount = (srvData[nSrvIndex++] & 0xFF);
                    for (int i = 0; i < 3; i++)
                    {
                        nAdvCount = (nAdvCount << 8);
                        nAdvCount += (srvData[nSrvIndex] & 0xFF);
                        nSrvIndex++;
                    }

                } else if (beaconType == BeaconPerperal.BEACON_TYPE_EDDY_UID) {
                    if (srvData.length < BeaconPerperal.MIN_EDDY_UID_LEN) {
                        return;
                    }
                    nBeaconPower = srvData[nSrvIndex++];

                    strUidNamesapceID = String.format("0x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
                            srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++],
                            srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++],
                            srvData[nSrvIndex++], srvData[nSrvIndex++]);

                    strUidSerialID = String.format("0x%02x%02x%02x%02x%02x%02x",
                            srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++], srvData[nSrvIndex++],
                            srvData[nSrvIndex++], srvData[nSrvIndex++]);
                } else if (beaconType == BeaconPerperal.BEACON_TYPE_EDDY_URL) {
                    if (srvData.length < BeaconPerperal.MIN_EDDY_URL_LEN) {
                        return;
                    }
                    nBeaconPower = srvData[nSrvIndex++];

                    char []urlCharEnc = new char[18];
                    int j = 0, k = 0;
                    for (j = nSrvIndex, k = 0; j < srvData.length; j++) {
                        urlCharEnc[k++] += (char) srvData[j];
                    }
                    char []urlCharDec = new char[40];
                    int nDecLen = Utils.EddystoneBeacon_decodeURL(urlCharEnc, k, urlCharDec);
                    if (nDecLen == 0){
                        strUrl = "N/A";
                    }else{
                        strUrl = "";
                        for (int i = 0; i < nDecLen; i++){
                            strUrl += urlCharDec[i];
                        }
                    }

                }
            }
        }

        if (record.getServiceData() != null)
        {
            List<ParcelUuid> lstSrvUUID = record.getServiceUuids();
            if (lstSrvUUID != null) {
                for (ParcelUuid uuid : lstSrvUUID) {
                    if (uuid.equals(Utils.PARCE_CFG_UUID_EDDYSTONE)) {
                        if (beaconType == BeaconPerperal.BEACON_TYPE_INVALID) {
                            beaconType = BeaconPerperal.BEACON_TYPE_CFG_MODE;
                        }
                        break;
                    }
                }
            }
        }

        //if has response data, then it is connectable
        boolean bConnEnable = true;
        if (strDevName == null){
            bConnEnable = false;
        }

        if (beaconType == BeaconPerperal.BEACON_TYPE_INVALID){
            return;
        }

        BeaconPerperal blePerp = mBLESrvMgr.getTrackByMac(strMacAddress);
        boolean bNewPerp = false;
        if (blePerp == null) {
            blePerp = mBLESrvMgr.createBlePerp(device);
            bNewPerp = true;
        } else {
            bNewPerp = false;
        }

        //update rssi
        long currTick = System.currentTimeMillis();
        blePerp.updateAdvRSSI(rssi, currTick);
        blePerp.updateAdvName(strDevName);
        blePerp.updateConnable(bConnEnable);

        if (beaconType == BeaconPerperal.BEACON_TYPE_IBEACON)
        {
            blePerp.updateIBeaconInfo(strUUID, strMajor, strMinor, nBeaconPower);
        }
        else if (beaconType == BeaconPerperal.BEACON_TYPE_EDDY_URL)
        {
            blePerp.updateEddystoneURL(strUrl, nBeaconPower);
        }
        else if (beaconType == BeaconPerperal.BEACON_TYPE_EDDY_UID)
        {
            blePerp.updateUidInfo(strUidNamesapceID, strUidSerialID, nBeaconPower);
        }
        else if (beaconType ==BeaconPerperal.BEACON_TYPE_EDDY_TLM)
        {
            blePerp.updateEddystoneTLM(nBatteryLevel, fTemputure, nAdvCount);
        }
        else if (beaconType ==BeaconPerperal.BEACON_TYPE_CFG_MODE)
        {
            blePerp.updateConfigMode(strDevName);
        }

        if (bNewPerp)
        {
            mHandler.sendEmptyMessageDelayed(MSG_REFERASH_VIEW, 200);
        }
        else
        {
            //chk need show
            final int position = getPositionByMac(blePerp.getMacAddress());
            if (position == -1){
                return;
            }

            mHandler.sendEmptyMessageDelayed(MSG_REFERASH_VIEW, 200);
            /*
            Message msg = mHandler.obtainMessage(MSG_REFERASH_ONE_ITEM_VIEW, position);
            mHandler.sendMessageDelayed(msg, 100);
            */
        }
    }

    public void updateListView()
    {
        mListViewAdapter.notifyDataSetChanged();
    }

    public void toastShow(String strMsg)
    {
        Toast toast=Toast.makeText(this, strMsg, Toast.LENGTH_SHORT);

        toast.setGravity(Gravity.CENTER, 0, 0);

        toast.show();
    }

    public int getDeviceSize(){
        return mBLESrvMgr.getDeviceArrSize();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            exitBy2Click(); //调用双击退出函数
        }
        return false;
    }
    /**
     * 双击退出函数
     */
    private static Boolean isExit = false;

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务

        } else {
            finish();
            System.exit(0);
        }
    }

    private class EditChangedListener implements TextWatcher {
        private CharSequence temp;//监听前的文本
        private int editStart;//光标开始位置
        private int editEnd;//光标结束位置
        private final int charMaxNum = 10;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            temp = s;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String strFltName = mEditFltDevName.getText().toString();
            mFilterName = strFltName.toLowerCase();
            if (mBLESrvMgr.filterByName(strFltName)){
                updateListView();
            }
        }
    };
}