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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private final static String TAG = "Beacon.DevAct";//DeviceScanActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final int FILE_RESULT = 0X7878;

    private static final String ACTION_MENU_UPLOAD = "101";
    private static final String ACTION_MENU_CONN= "102";


    BeaconPerperal mBlePerp;
    private TextView mConnectionState;
    private LinearLayout mConnLayoutView;
    private UploadDialog mUpdateDialog;

    private RadioButton mRadioAdvTypeIBeacon, mRadioAdvTypeEddyURL, mRadioAdvTypeEddyUID, mRadioAdvTypeEddyTLM;
    private EditText mIBeaconUUID, mIBeaconMajorID, mIBeaconMinorID;
    private EditText mEddyURL;
    private EditText mEddyNamespaceID, mEddySerialID;
    private Switch mSwitchConnEnable;
    private Button mActionButton;

    private EditText mEditAdvPeriod;
    RadioButton []mRadioPwLvl;
    private EditText mEditNewPassword;
    private EditText mEditDevName, mEditDevSerialID;
    private LinearLayout mIbeaconView, mEddyUrlView, mEddyUidView;

    private String mDeviceAddress;
    private BeaconPerpMgr mBluetoothLeService;
    private SharePreferenceMgr mPrefCfg;


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(BeaconPerperal.BEACON_MAC_ADDRESS);
        mBluetoothLeService = BeaconPerpMgr.shareInstance(this);
        mPrefCfg = SharePreferenceMgr.shareInstance(this);
        mBlePerp = mBluetoothLeService.getTrackByMac(mDeviceAddress);


        // Sets up UI references.
        mConnectionState = (TextView)findViewById(R.id.connection_states);

        mConnLayoutView = (LinearLayout)findViewById(R.id.beaconConnSetting);

        //初始化升级对话框
        mUpdateDialog = new UploadDialog(this);
        mBluetoothLeService.startConnect(mDeviceAddress);

        final RadioGroup advTypeGroup=(RadioGroup)findViewById(R.id.radioAdvTypeGroup);
        advTypeGroup.setOnCheckedChangeListener(this);// 当然也可以使用匿名内部类实现

        mIbeaconView = (LinearLayout) findViewById(R.id.iBeaconSetting);
        mEddyUrlView = (LinearLayout) findViewById(R.id.EddystoneURLSetting);
        mEddyUidView = (LinearLayout) findViewById(R.id.EddystoneUIDSetting);

        //beacon type
        mRadioAdvTypeIBeacon = (RadioButton) findViewById(R.id.radioAdvTypeIBeacon);
        mRadioAdvTypeEddyURL = (RadioButton) findViewById(R.id.radioAdvTypeURL);
        mRadioAdvTypeEddyUID = (RadioButton) findViewById(R.id.radioAdvTypeUID);
        mRadioAdvTypeEddyTLM = (RadioButton) findViewById(R.id.radioAdvTypeTLM);

        mIBeaconUUID = (EditText) findViewById(R.id.editIBeaconUUID);
        mIBeaconMajorID = (EditText) findViewById(R.id.editIBeaconMajor);
        mIBeaconMinorID = (EditText) findViewById(R.id.editIBeaconMinor);

        //url setting
        mEddyURL = (EditText) findViewById(R.id.editEddyUrl);

        //uid
        mEddyNamespaceID = (EditText) findViewById(R.id.editEddyNamespaceID);
        mEddySerialID = (EditText) findViewById(R.id.editEddySerialID);

        mEditAdvPeriod = (EditText) findViewById(R.id.editBeaconAdvPeriod);

        findViewById(R.id.imageButtonBack).setOnClickListener(this);
        mActionButton = (Button) findViewById(R.id.buttonAction);
        mActionButton.setOnClickListener(this);
        TextView deviceName = (TextView)findViewById(R.id.textViewDeviceName);
        deviceName.setText(mBlePerp.getDeviceName());

        (findViewById(R.id.buttonSaveAsTemplate)).setOnClickListener(this);
        (findViewById(R.id.buttonLoadTemplate)).setOnClickListener(this);

        mRadioPwLvl = new RadioButton[4];
        mRadioPwLvl[0] = (RadioButton) findViewById(R.id.radioTxPowerNeg20dBm);
        mRadioPwLvl[1] = (RadioButton) findViewById(R.id.radioTxPowerNeg10dBm);
        mRadioPwLvl[2] = (RadioButton) findViewById(R.id.radioTxPower0dBm);
        mRadioPwLvl[3] = (RadioButton) findViewById(R.id.radioTxPower5dBm);

        mEditDevName = (EditText)findViewById(R.id.editBeaconname);
        mEditDevSerialID = (EditText)findViewById(R.id.editBeaconSerialID);

        mEditNewPassword = (EditText)findViewById(R.id.editNewPassword);
        mEditNewPassword.setText(mPrefCfg.getSingleBeaconPassword(mDeviceAddress));

        mSwitchConnEnable = (Switch)findViewById(R.id.switchConnEnalbe);

        mConnLayoutView.setVisibility(View.GONE);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BeaconPerperal.BEACON_APP_CHG.equals(action)
                    || BeaconPerperal.BEACON_CONN_STATUS_CHG.equals(action)) {
                String strMacAddr = intent.getStringExtra(BeaconPerperal.BEACON_MAC_ADDRESS);
                if (strMacAddr == null) {
                    return;
                }
                if (!strMacAddr.equals(mDeviceAddress)) {
                    return;
                }

                if (BeaconPerperal.BEACON_CONN_STATUS_CHG.equals(action)) {

                    int nOldConnStatus = intent.getIntExtra(BeaconPerperal.BEACON_OLD_CONN_STATUS,
                            BeaconPerperal.BEACON_CONN_STATUS_INVALID);
                    int nNewConnStatus = intent.getIntExtra(BeaconPerperal.BEACON_NEW_CONN_STATUS,
                            BeaconPerperal.BEACON_CONN_STATUS_INVALID);


                    //connect timeout
                    if (nOldConnStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTING
                            && nNewConnStatus == BeaconPerperal.BEACON_CONN_STATUS_DISCONNECTED) {
                        new AlertDialog.Builder(DeviceControlActivity.this)
                                .setTitle(R.string.conn_error_title)
                                .setMessage(R.string.connect_error_timeout)
                                .setPositiveButton(R.string.Dialog_OK, null)
                                .show();

                    }

                    updateConStatusView();

                    //update data
                    if (nOldConnStatus == BeaconPerperal.BEACON_CONN_STATUS_DISCOV_SRV
                            && nNewConnStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTED) {
                        updateViewFromPerp();
                    }
                    if (nOldConnStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTED
                            && nNewConnStatus == BeaconPerperal.BEACON_CONN_STATUS_DISCONNECTED) {
                        if (mUpdateDialog.isShowing()){
                            mUpdateDialog.dismiss();
                        }
                    }
                }else if (BeaconPerperal.BEACON_APP_CHG.equals(action)) {
                    int nBeaconStatus = intent.getIntExtra(BeaconPerperal.BEACON_APP_VALUE, BeaconPerperal.BEACON_NTF_INVALID);

                    if (BeaconPerperal.BEACON_NTF_WR_COMPLETE == nBeaconStatus) {
                        mUpdateDialog.dismiss();
                        new AlertDialog.Builder(DeviceControlActivity.this)
                                .setTitle(R.string.upload_data_title)
                                .setMessage(R.string.upload_data_success)
                                .setPositiveButton(R.string.Dialog_OK, null)
                                .show();
                    }
                    else if (BeaconPerperal.BEACON_NTF_RD_COMPLETE == nBeaconStatus) {
                        updateViewFromPerp();
                    }
                    else if (BeaconPerperal.BEACON_NTF_AUTH_FAILED == nBeaconStatus) {
                        mUpdateDialog.dismiss();
                        final EditText inputServer = new EditText(DeviceControlActivity.this);
                        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceControlActivity.this);
                        builder.setTitle(getString(R.string.auth_error_title));
                        builder.setView(inputServer);
                        builder.setNegativeButton(R.string.Dialog_Cancel, null);
                        builder.setPositiveButton(R.string.Dialog_OK, null);
                        final AlertDialog alertDialog = builder.create();
                        alertDialog.show();

                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String strNewPassword = inputServer.getText().toString().trim();
                                if (strNewPassword.length() != 6) {
                                    Toast.makeText(DeviceControlActivity.this,
                                            R.string.connect_error_auth_format,
                                            Toast.LENGTH_SHORT).show();
                                } else {

                                    //update password
                                    mPrefCfg.setSingleBeaconPassword(mBlePerp.getMacAddress(), strNewPassword);
                                    mEditNewPassword.setText(strNewPassword);
                                    alertDialog.dismiss();

                                    //重连
                                    mBluetoothLeService.startConnect(mDeviceAddress);
                                }
                            }
                        });
                    }else if (BeaconPerperal.BEACON_NTF_WR_READ_CHECK_FAIL == nBeaconStatus){
                        mUpdateDialog.dismiss();

                        new AlertDialog.Builder(DeviceControlActivity.this)
                                .setTitle(R.string.common_error_title)
                                .setMessage(R.string.err_read_check_failed)
                                .setPositiveButton(R.string.Dialog_OK, null)
                                .show();
                        updateViewFromPerp();
                    }else if (BeaconPerperal.BEACON_NTF_MO_CFG_CHANGE == nBeaconStatus){
                        mUpdateDialog.dismiss();

                        new AlertDialog.Builder(DeviceControlActivity.this)
                                .setTitle(R.string.common_error_title)
                                .setMessage(R.string.err_no_data_chage_need_cfg)
                                .setPositiveButton(R.string.Dialog_OK, null)
                                .show();
                    }else if (BeaconPerperal.BEACON_NTF_WR_UNLOCK_CMP_PROGRESS == nBeaconStatus)
                    {
                        mUpdateDialog.setPercentText(getString(R.string.upload_write_paramaters));
                        mUpdateDialog.setPercent(0);
                    }
                    else if (BeaconPerperal.BEACON_NTF_WR_CFG_PROGRESS == nBeaconStatus)
                    {
                        int nPencent = intent.getIntExtra(BeaconPerperal.BEACON_EXT_VALUE,
                                BeaconPerperal.BEACON_NTF_INVALID);
                        mUpdateDialog.setPercentText(getString(R.string.upload_write_paramaters));
                        mUpdateDialog.setPercent(nPencent/2);
                    }
                    else if (BeaconPerperal.BEACON_NTF_RD_CFG_PROGRESS == nBeaconStatus)
                    {
                        int nPencent = intent.getIntExtra(BeaconPerperal.BEACON_EXT_VALUE,
                                BeaconPerperal.BEACON_NTF_INVALID);
                        mUpdateDialog.setPercentText(getString(R.string.upload_read_check_paramaters));
                        mUpdateDialog.setPercent(50 + nPencent/2);
                    }
                }
            }
        }
    };

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {// Q: 参数 group 暂时还没搞清什么用途
        switch (checkedId) {
            case R.id.radioAdvTypeIBeacon:
                mIbeaconView.setVisibility(View.VISIBLE);
                mEddyUrlView.setVisibility(View.GONE);
                mEddyUidView.setVisibility(View.GONE);

                mIBeaconUUID.setText(mBlePerp.getCfgIBeaconUUID());
                mIBeaconMajorID.setText(mBlePerp.getCfgMajorID());
                mIBeaconMinorID.setText(mBlePerp.getCfgMinorID());
                break;

            case R.id.radioAdvTypeURL:
                mIbeaconView.setVisibility(View.GONE);
                mEddyUrlView.setVisibility(View.VISIBLE);
                mEddyUidView.setVisibility(View.GONE);

                mEddyURL.setText(mBlePerp.getCfgEddyUrl());

                break;
            case R.id.radioAdvTypeUID:
                mIbeaconView.setVisibility(View.GONE);
                mEddyUrlView.setVisibility(View.GONE);
                mEddyUidView.setVisibility(View.VISIBLE);

                mEddyNamespaceID.setText(mBlePerp.getCfgEddyNamespaceID());
                mEddySerialID.setText(mBlePerp.getCfgEddySerialID());

                break;
            case R.id.radioAdvTypeTLM:
                mIbeaconView.setVisibility(View.GONE);
                mEddyUrlView.setVisibility(View.GONE);
                mEddyUidView.setVisibility(View.GONE);
                break;
            default:
                Toast.makeText(this, "secret", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void updateViewToPerp()
    {
        //remove config
        mBlePerp.clearForWriteCfg();

        //set type
        if (mRadioAdvTypeEddyTLM.isChecked()){
            mBlePerp.setCfgBeaconAdvType(BeaconPerperal.BEACON_TYPE_EDDY_TLM);
        }else if (mRadioAdvTypeEddyURL.isChecked()) {
            mBlePerp.setCfgBeaconAdvType(BeaconPerperal.BEACON_TYPE_EDDY_URL);
        }else if (mRadioAdvTypeEddyUID.isChecked()) {
            mBlePerp.setCfgBeaconAdvType(BeaconPerperal.BEACON_TYPE_EDDY_UID);
        }else if (mRadioAdvTypeIBeacon.isChecked()) {
            mBlePerp.setCfgBeaconAdvType(BeaconPerperal.BEACON_TYPE_IBEACON);
        }

        boolean bySetResult = false;
        if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_IBEACON) {
            bySetResult = mBlePerp.setCfgIBeaconUUID(mIBeaconUUID.getText().toString());
            if (!bySetResult){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.IBEACON_UUID_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }

            bySetResult = mBlePerp.setCfgMajorID(mIBeaconMajorID.getText().toString());
            if (!bySetResult){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.IBEACON_MAJOR_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }

            bySetResult = mBlePerp.setCfgMinorID(mIBeaconMinorID.getText().toString());
            if (!bySetResult){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.IBEACON_MINOR_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }
        }else if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_EDDY_UID){
            bySetResult = mBlePerp.setCfgEddyNamespaceID(mEddyNamespaceID.getText().toString());
            if (!bySetResult){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.EDDY_NAMESPACE_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }

            bySetResult = mBlePerp.setCfgEddySerialID(mEddySerialID.getText().toString());
            if (!bySetResult){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.EDDY_SERIAL_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }
        }else if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_EDDY_URL) {
            int nResult = mBlePerp.setCfgEddyUrl(mEddyURL.getText().toString());
            if (nResult == 0){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.EDDY_URL_FMT_ERROR)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }
            else if (nResult == -1){
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_error_title)
                        .setMessage(R.string.EDDY_URL_FMT_LEN_18)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                return;
            }
        }

        //adv period
        if (!mBlePerp.setCfgAdvPeriod(mEditAdvPeriod.getText().toString())){
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle(R.string.common_error_title)
                    .setMessage(R.string.BEACON_ADV_PERIOD_FMT_ERROR)
                    .setPositiveButton(R.string.Dialog_OK, null)
                    .show();
            return;
        }

        for (int i = 0; i < 4; i++){
            if (mRadioPwLvl[i].isChecked()){
                mBlePerp.setCfgBeaconTxPowerIdx(i);
                break;
            }
        }

        if (!mBlePerp.setCfgBeaconName(mEditDevName.getText().toString())){
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle(R.string.common_error_title)
                    .setMessage(R.string.BEACON_DEV_NAME_FMT_ERROR)
                    .setPositiveButton(R.string.Dialog_OK, null)
                    .show();
            return;
        }

        if (!mBlePerp.setCfgBeaconSerialID(mEditDevSerialID.getText().toString())){
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle(R.string.common_error_title)
                    .setMessage(R.string.BEACON_DEV_SERIAL_ID_FMT_ERROR)
                    .setPositiveButton(R.string.Dialog_OK, null)
                    .show();
            return;
        }

        //fill new password
        String strNewPassword = mEditNewPassword.getText().toString();
        if (!strNewPassword.equals("") && strNewPassword.length() != 6)
        {
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle(R.string.common_error_title)
                    .setMessage(R.string.connect_error_auth_format)
                    .setPositiveButton(R.string.Dialog_OK, null)
                    .show();
            return;
        }
        String strOldPassword = mPrefCfg.getSingleBeaconPassword(mDeviceAddress);
        if (strNewPassword.length() == 6
                && !strOldPassword.equals(strNewPassword)){
            mBlePerp.setCfgNewLockPassword(strNewPassword);
        }

        if (mSwitchConnEnable.isChecked()) {
            mBlePerp.setCfgBeaconAdvFlag(1);
        }else{
            mBlePerp.setCfgBeaconAdvFlag(0);
        }
        mSwitchConnEnable.setChecked(mBlePerp.getCfgBeaconAdvFlag() != 0);

        //重连
        if (!mBlePerp.isDeviceLocked()) {
            mBlePerp.startUploadCfgParam2Device(strNewPassword);

            mUpdateDialog.show();
            //mUpdateDialog.setNoticeText(getString(R.string.upload_config_paramaters));
            mUpdateDialog.setPercentText(getString(R.string.upload_unlock_for_write));
        }else{
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle(R.string.common_error_title)
                    .setMessage(R.string.write_error_dev_unlocked)
                    .setPositiveButton(R.string.Dialog_OK, null)
                    .show();
        }

    }

    public void updateViewFromPerp()
    {
        mRadioAdvTypeIBeacon.setChecked(false);
        mRadioAdvTypeEddyURL.setChecked(false);
        mRadioAdvTypeEddyUID.setChecked(false);
        mRadioAdvTypeEddyTLM.setChecked(false);
        mIbeaconView.setVisibility(View.GONE);
        mEddyUidView.setVisibility(View.GONE);
        mEddyUrlView.setVisibility(View.GONE);
        if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_IBEACON) {
            mRadioAdvTypeIBeacon.setChecked(true);
            mIbeaconView.setVisibility(View.VISIBLE);

            mIBeaconUUID.setText(mBlePerp.getCfgIBeaconUUID());
            mIBeaconMajorID.setText(mBlePerp.getCfgMajorID());
            mIBeaconMinorID.setText(mBlePerp.getCfgMinorID());

        }else if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_EDDY_UID){
            mRadioAdvTypeEddyUID.setChecked(true);
            mEddyUidView.setVisibility(View.VISIBLE);
            mEddyNamespaceID.setText(mBlePerp.getCfgEddyNamespaceID());
            mEddySerialID.setText(mBlePerp.getCfgEddySerialID());
        }else if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_EDDY_URL) {
            mRadioAdvTypeEddyURL.setChecked(true);
            mEddyUrlView.setVisibility(View.VISIBLE);
            mEddyURL.setText(mBlePerp.getCfgEddyUrl());
        }else if (mBlePerp.getCfgBeaconAdvType() == BeaconPerperal.BEACON_TYPE_EDDY_TLM) {
            mRadioAdvTypeEddyTLM.setChecked(true);
        }

        mSwitchConnEnable.setChecked(mBlePerp.getCfgBeaconAdvFlag() != 0);

        mEditAdvPeriod.setText(String.valueOf(mBlePerp.getCfgAdvPeriod()));

        for (int i = 0; i < 4; i++){
            String strTxPower = mBlePerp.getCfgBeaconTxPower(i);
            if (strTxPower != null) {
                mRadioPwLvl[i].setText(strTxPower);
            }

            if (i == mBlePerp.getCfgBeaconTxPowerIdx()) {
                mRadioPwLvl[i].setChecked(true);
            }else{
                mRadioPwLvl[i].setChecked(false);
            }
        }

        //mEditNewPassword.setText(new String(mBlePerp.mCfgLockCode));
        mEditDevName.setText(mBlePerp.getCfgBeaconName());
        mEditDevSerialID.setText(mBlePerp.getCfgBeaconSerialID());
    }

    public void updateConStatusView()
    {
        int nConnStatus = mBlePerp.getConnStatus();
        switch (nConnStatus) {
            case BeaconPerperal.BEACON_CONN_STATUS_DISCONNECTED:
                mConnectionState.setText(R.string.disconnected);
                mConnectionState.setTextColor(Color.RED);
                break;
            case BeaconPerperal.BEACON_CONN_STATUS_CONNECTED:
                mConnectionState.setText(R.string.connected);
                mConnectionState.setTextColor(Color.BLUE);
                break;
            case BeaconPerperal.BEACON_CONN_STATUS_DISCOV_SRV:
                mConnectionState.setText(R.string.DISCOVER_SRV);
                mConnectionState.setTextColor(Color.BLUE);
                break;

            case BeaconPerperal.BEACON_CONN_STATUS_CONNECTING:
                mConnectionState.setText(R.string.connecting);
                mConnectionState.setTextColor(Color.BLUE);
                break;
            default:
                mConnectionState.setText(R.string.unknown);
                mConnectionState.setTextColor(Color.RED);
        }

        if (nConnStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTED) {
            mConnLayoutView.setVisibility(View.VISIBLE);
        }else{
            mConnLayoutView.setVisibility(View.GONE);
        }

        //invalidateOptionsMenu();
        updateActionButton();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.imageButtonBack:
            {
                mBlePerp.closeConnection();
                onBackPressed();
                break;
            }

            case R.id.buttonAction:{
                if (mActionButton.getTag() == ACTION_MENU_UPLOAD)
                {
                    updateViewToPerp();
                }
                else if (mActionButton.getTag() == ACTION_MENU_CONN) {
                    mBluetoothLeService.startConnect(mDeviceAddress);
                }
                break;
            }
            case R.id.buttonSaveAsTemplate:
                saveConfigParamToDB();
                new AlertDialog.Builder(DeviceControlActivity.this)
                        .setTitle(R.string.common_succ_title)
                        .setMessage(R.string.beacon_save_para_to_template_success)
                        .setPositiveButton(R.string.Dialog_OK, null)
                        .show();
                break;

            case R.id.buttonLoadTemplate:
                if (!loadConfigParamFromDB()) {
                    new AlertDialog.Builder(DeviceControlActivity.this)
                            .setTitle(R.string.common_error_title)
                            .setMessage(R.string.beacon_load_para_to_template_failed)
                            .setPositiveButton(R.string.Dialog_OK, null)
                            .show();
                }else{
                    toastShow(this, R.string.beacon_load_para_to_template_success);
                }
                break;

            default:
        }
    }

    private boolean loadConfigParamFromDB()
    {
        if (!mPrefCfg.getTemplateExist()){
            return false;
        }

        int nAdvType = mPrefCfg.getAdvType();
        mIbeaconView.setVisibility(View.GONE);
        mEddyUidView.setVisibility(View.GONE);
        mEddyUrlView.setVisibility(View.GONE);

        if ( nAdvType== BeaconPerperal.BEACON_TYPE_IBEACON) {
            mRadioAdvTypeIBeacon.setChecked(true);
            mIbeaconView.setVisibility(View.VISIBLE);

            mIBeaconUUID.setText(mPrefCfg.getIBeaconUUID());
            mIBeaconMajorID.setText(mPrefCfg.getIBeaconMajorID());
            mIBeaconMinorID.setText(mPrefCfg.getIBeaconMinorID());

        }else if (nAdvType == BeaconPerperal.BEACON_TYPE_EDDY_UID){
            mRadioAdvTypeEddyUID.setChecked(true);
            mEddyUidView.setVisibility(View.VISIBLE);
            mEddyNamespaceID.setText(mPrefCfg.getEddyNamespace());
            mEddySerialID.setText(mPrefCfg.getEddySerial());
        }else if (nAdvType == BeaconPerperal.BEACON_TYPE_EDDY_URL) {
            mRadioAdvTypeEddyURL.setChecked(true);
            mEddyUrlView.setVisibility(View.VISIBLE);
            mEddyURL.setText(mPrefCfg.getEddyUrl());
        }else if (nAdvType == BeaconPerperal.BEACON_TYPE_EDDY_TLM) {
            mRadioAdvTypeEddyTLM.setChecked(true);
        }

        mEditAdvPeriod.setText(mPrefCfg.getAdvPeriod());

        for (int i = 0; i < 4; i++){
            String strPwlLvl = mPrefCfg.getTxPwlLvl(i);
            mRadioPwLvl[i].setText(strPwlLvl);

            if (i == mPrefCfg.getTxPwlIdx()) {
                mRadioPwLvl[i].setChecked(true);
            }else{
                mRadioPwLvl[i].setChecked(false);
            }
        }
        mEditDevName.setText(mPrefCfg.getBeaconName());
        mEditDevSerialID.setText(mPrefCfg.getBeaconSerialID());
        mSwitchConnEnable.setChecked(mPrefCfg.getConnEnable());
        mEditNewPassword.setText(mPrefCfg.getCommonBeaconPassword());

        return true;
    }

    private void saveConfigParamToDB()
    {
        //set type
        int nAdvType = 0;
        if (mRadioAdvTypeEddyTLM.isChecked()){
            nAdvType = BeaconPerperal.BEACON_TYPE_EDDY_TLM;
        }else if (mRadioAdvTypeEddyURL.isChecked()) {
            nAdvType = BeaconPerperal.BEACON_TYPE_EDDY_URL;
        }else if (mRadioAdvTypeEddyUID.isChecked()) {
            nAdvType = BeaconPerperal.BEACON_TYPE_EDDY_UID;
        }else if (mRadioAdvTypeIBeacon.isChecked()) {
            nAdvType = BeaconPerperal.BEACON_TYPE_IBEACON;
        }
        mPrefCfg.saveAdvType(nAdvType);

        //save paramaters
        if (nAdvType == BeaconPerperal.BEACON_TYPE_IBEACON) {
            mPrefCfg.saveIBeaconUUID(mIBeaconUUID.getText().toString());
            mPrefCfg.saveIBeaconMajorID(mIBeaconMajorID.getText().toString());
            mPrefCfg.saveIBeaconMinorID(mIBeaconMinorID.getText().toString());
        }else if (nAdvType == BeaconPerperal.BEACON_TYPE_EDDY_UID){
            mPrefCfg.saveEddyNamespace(mEddyNamespaceID.getText().toString());
            mPrefCfg.saveEddySerial(mEddySerialID.getText().toString());
        }else if (nAdvType == BeaconPerperal.BEACON_TYPE_EDDY_URL) {
            mPrefCfg.saveEddyUrl(mEddyURL.getText().toString());
        }

        mPrefCfg.saveAdvPeriod(mEditAdvPeriod.getText().toString());

        //power level
        for (int i = 0; i < 4; i++){
            mPrefCfg.saveTxPwlLvl(i, mRadioPwLvl[i].getText().toString());
            if (mRadioPwLvl[i].isChecked()){
                mPrefCfg.saveTxPwlIdx(i);
            }
        }

        mPrefCfg.saveConnEnable(mSwitchConnEnable.isChecked());

        mPrefCfg.saveBeaconName(mEditDevName.getText().toString());

        mPrefCfg.saveBeaconSerialID(mEditDevSerialID.getText().toString());

        mPrefCfg.saveCommonBeaconPassword(mEditNewPassword.getText().toString());

        mPrefCfg.saveTemplateExist(true);
    }



    public static void toastShow(Context parent, int strId)
    {
        Toast toast=Toast.makeText(parent, strId, Toast.LENGTH_SHORT);

        toast.setGravity(Gravity.CENTER, 0, 0);

        toast.show();
        return;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBlePerp != null){
            mBlePerp.closeConnection();
        }
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        int nDevStatus = mBlePerp.getConnStatus();
        if (nDevStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTED) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            menu.findItem(R.id.menu_upload).setVisible(true);
        }
        else if (nDevStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTING
            || nDevStatus == BeaconPerperal.BEACON_CONN_STATUS_DISCOV_SRV){
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_upload).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else{
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_upload).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
        return true;
    }

    public void updateActionButton()
    {
        int nDevStatus = mBlePerp.getConnStatus();
        if (nDevStatus == BeaconPerperal.BEACON_CONN_STATUS_CONNECTED) {
            mActionButton.setVisibility(View.VISIBLE);
            mActionButton.setText(R.string.devMenuUpload);
            mActionButton.setTag(ACTION_MENU_UPLOAD);
        }
        else if (nDevStatus == BeaconPerperal.BEACON_CONN_STATUS_DISCONNECTED) {
            mActionButton.setVisibility(View.VISIBLE);
            mActionButton.setText(R.string.menu_connect);
            mActionButton.setTag(ACTION_MENU_CONN);
        }
        else
        {
            mActionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.startConnect(mDeviceAddress);
                return true;
            case R.id.menu_upload:
                updateViewToPerp();
                return true;

            case android.R.id.home:
                mBlePerp.closeConnection();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BeaconPerperal.BEACON_APP_CHG);
        intentFilter.addAction(BeaconPerperal.BEACON_CONN_STATUS_CHG);

        return intentFilter;
    }
}
