package com.beacon.kbeacon.KBeaconConfigTools;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class SettingActivity extends Activity {

    private TextView mVryCodeView;
    private TextView mMinRssiValueView;
    private TextView mAppVersion;

    private SharePreferenceMgr mPrefMgr = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_setting);
        mPrefMgr = SharePreferenceMgr.shareInstance(getApplication());

        //mVryCodeView = (TextView)findViewById(R.id.editTextVryCode);
        mVryCodeView.setText(mPrefMgr.getDefaultPassword());

        //mMinRssiValueView = (TextView)findViewById(R.id.editMinRSSIValue);

        //mAppVersion = (TextView)findViewById(R.id.textVersionTitle);
        mAppVersion.setText(getVersionName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            saveProfile();

            finish();
        }
        if (id == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public String getVersionName()
    {
        try
        {
            PackageManager packageManager = this.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    this.getPackageName(), 0);
            return packageInfo.versionName;

        } catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private void saveProfile(){
        /*
        String strRssiValue = mMinRssiTextView.getText().toString();
        int RssiValue = Integer.parseInt(strRssiValue);
        mPrefMgr.setRssiValue(RssiValue);

        String strMaxTestNum = mMaxTestNumTextView.getText().toString();
        int maxTestNum = Integer.parseInt(strMaxTestNum);
        mPrefMgr.setTestNum(maxTestNum);
*/
        String strRssiValue = mMinRssiValueView.getText().toString();
        int RssiValue = Integer.parseInt(strRssiValue);

        String strPassword = mVryCodeView.getText().toString();
        mPrefMgr.setDefaultPassword(strPassword);

        /*
        boolean bSingleTestMode = mSingleFlashTestMode.isChecked();
        mPrefMgr.setSingleTestMode(bSingleTestMode);

        String strAliveTick = mAliveView.getText().toString();
        int nAliveTick = Integer.parseInt(strAliveTick);
        mPrefMgr.setAliveTick(nAliveTick);

        mPrefMgr.setOthTrackName(mTrackNameView.getText().toString());

        String strMusicIdx = mMusicIndexView.getText().toString();
        long nMusicIdex = Long.parseLong(strMusicIdx);
        mPrefMgr.setMusiIndex(nMusicIdex);

        */
    }
}
