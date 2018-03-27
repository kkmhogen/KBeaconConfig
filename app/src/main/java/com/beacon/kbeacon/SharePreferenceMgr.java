package com.beacon.kbeacon;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hogen on 15/9/22.
 */
public class SharePreferenceMgr {
    private Context mContext;
    private final static String SETTING_INFO = "SETTING_INFO";
    private final static String FLD_VRY_CODE_NUM_VALUE = "VRY_CODE_NUM_VALUE";
    private final static String FLD_ALIVE_TICK = "DEF_ALIVE_TICK_FIELD";
    private final static String DEF_VRY_CODE_NUM = "000000";
    private final static long DEF_ALIVE_TICK_S = 6000;
    private String mDefaultPassword = DEF_VRY_CODE_NUM;
    private long mAliveTickS = DEF_ALIVE_TICK_S;


    static private SharePreferenceMgr sPrefMgr = null;

   static public synchronized SharePreferenceMgr shareInstance(Context ctx){
        if (sPrefMgr == null){
            sPrefMgr = new SharePreferenceMgr();
            sPrefMgr.initSetting(ctx);
        }

        return sPrefMgr;
    }

    private SharePreferenceMgr(){
    }

    //初始化配置
    public void initSetting(Context ctx){
        mContext = ctx;

        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);

        mDefaultPassword = shareReference.getString(FLD_VRY_CODE_NUM_VALUE, DEF_VRY_CODE_NUM);

        mAliveTickS = shareReference.getLong(FLD_ALIVE_TICK, DEF_ALIVE_TICK_S);



    }

    public void saveAdvType(int nAdvIdx){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putInt("saveAdvType", nAdvIdx);
        edit.apply();
    }

    public int getAdvType(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getInt("saveAdvType", 0);
    }

    public void saveIBeaconUUID(String strUUID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveIBeaconUUID", strUUID);
        edit.apply();
    }

    public String getIBeaconUUID(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveIBeaconUUID", "");
    }

    public void saveIBeaconMajorID(String strUUID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveIBeaconMajorID", strUUID);
        edit.apply();
    }

    public String getIBeaconMajorID(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveIBeaconMajorID", "");
    }

    public void saveIBeaconMinorID(String strUUID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveIBeaconMinorID", strUUID);
        edit.apply();
    }

    public String getIBeaconMinorID(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveIBeaconMinorID", "");
    }

    public void saveEddyNamespace(String strUUID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveEddyNamespace", strUUID);
        edit.apply();
    }

    public String getEddyNamespace(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveEddyNamespace", "");
    }

    public void saveEddySerial(String strUUID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveEddySerial", strUUID);
        edit.apply();
    }

    public String getEddySerial(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveEddySerial", "");
    }

    public void saveEddyUrl(String strURL){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveEddyUrl", strURL);
        edit.apply();
    }

    public String getEddyUrl(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveEddyUrl", "");
    }

    public void saveAdvPeriod(String strPeriod){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveAdvPeriod", strPeriod);
        edit.apply();
    }

    public String getAdvPeriod(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveAdvPeriod", "");
    }

    public void saveTxPwlLvl(int nTxPwIdx, String strPwlLvl){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveTxPwlLvl"+nTxPwIdx, strPwlLvl);
        edit.apply();
    }

    public String getTxPwlLvl(int nTxPwIdx){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveTxPwlLvl"+nTxPwIdx, "");
    }

    public void saveTxPwlIdx(int nTxPwIdx){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putInt("saveTxPwlIdx", nTxPwIdx);
        edit.apply();
    }

    public int getTxPwlIdx(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getInt("saveTxPwlIdx", 0);
    }



    public void saveConnEnable(boolean isEnalbeConn){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putBoolean("saveConnEnable", isEnalbeConn);
        edit.apply();
    }

    public boolean getConnEnable(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getBoolean("saveConnEnable", false);
    }

    public void saveBeaconName(String strPeriod){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveBeaconName", strPeriod);
        edit.apply();
    }

    public String getBeaconName(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveBeaconName", "");
    }

    public int getMinRssiFilter()
    {
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getInt("MinRssiFilter", -70);
    }

    public void saveMinRssiFilter(int rssiFilter){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putInt("MinRssiFilter", rssiFilter);
        edit.apply();
    }

    public int getMaxRssiFilter()
    {
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getInt("MaxRssiFilter", 10);
    }

    public void saveMaxRssiFilter(int rssiFilter){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putInt("MaxRssiFilter", rssiFilter);
        edit.apply();
    }

    public void saveBeaconSerialID(String serialID){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveBeaconSerialID", serialID);
        edit.apply();
    }

    public String getBeaconSerialID(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveBeaconSerialID", "");
    }

    public void saveCommonBeaconPassword(String strPeriod){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString("saveCommonBeaconPassword", strPeriod);
        edit.apply();
    }

    public void saveTemplateExist(boolean bExist){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putBoolean("saveTemplateExist", bExist);
        edit.apply();
    }

    public boolean getTemplateExist(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getBoolean("saveTemplateExist", false);
    }

    public String getCommonBeaconPassword(){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        return shareReference.getString("saveCommonBeaconPassword", "");
    }


    public void setSingleBeaconPassword(String strMacAddress, String strVryCode){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString(FLD_VRY_CODE_NUM_VALUE+strMacAddress.toLowerCase(), strVryCode);
        edit.apply();
    }

    public String getSingleBeaconPassword(String strMacAddress){
        String strPassword;

        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        strPassword = shareReference.getString(FLD_VRY_CODE_NUM_VALUE+strMacAddress.toLowerCase(), null);
        if (strPassword == null){
            strPassword = mDefaultPassword;
        }

        return strPassword;
    }

    public void setDefaultPassword(String nVryCode){
        SharedPreferences shareReference = mContext.getSharedPreferences(SETTING_INFO, mContext.MODE_PRIVATE);
        SharedPreferences.Editor edit = shareReference.edit();
        edit.putString(FLD_VRY_CODE_NUM_VALUE, nVryCode);
        edit.apply();

        mDefaultPassword = nVryCode;
    }

    public String getDefaultPassword(){
        return mDefaultPassword;
    }



    public long getAliveTick()
    {
        return mAliveTickS;
    }

}
