package com.beacon.kbeacon;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class LeDeviceListAdapter extends BaseAdapter {

	// Adapter for holding devices found through scanning.
	private DeviceScanActivity mContext;

	public LeDeviceListAdapter(DeviceScanActivity c) {
		super();
		mContext = c;
	}

	@Override
	public int getCount() {
		return mContext.getDeviceSize();
	}

	@Override
	public Object getItem(int i) {
		return mContext.getBlePerp(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}


	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;
		// General ListView optimization code.
		if (view == null) {
			view = LayoutInflater.from(mContext).inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceName = (TextView) view
					.findViewById(R.id.beacon_name);
			viewHolder.rssiState = (TextView) view
					.findViewById(R.id.beacon_rssi);

			/*viewHolder.deviceTxPower = (TextView) view
					.findViewById(R.id.beacon_tx_power);
			*/
			viewHolder.deviceBattery = (TextView) view
					.findViewById(R.id.beacon_battery);
			viewHolder.deviceMacAddr = (TextView) view
					.findViewById(R.id.beacon_mac_address);
			viewHolder.deviceConnEnable = (TextView) view
					.findViewById(R.id.beacon_temp);
			viewHolder.deviceConnEnable = (TextView) view
					.findViewById(R.id.beacon_conn_enable);
			viewHolder.deviceAdvRtt = (TextView) view
					.findViewById(R.id.beacon_rtt);
			viewHolder.deviceTempeture = (TextView) view
					.findViewById(R.id.beacon_temp);
			viewHolder.deviceMajor = (TextView) view
					.findViewById(R.id.beacon_major);
			viewHolder.deviceMinor = (TextView) view
					.findViewById(R.id.beacon_minor);
			viewHolder.deviceUUID = (TextView) view
					.findViewById(R.id.beacon_uuid);

			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		BeaconPerperal device = mContext.getBlePerp(i);
		if (device == null){
			return null;
		}

		if (device.getName() != null && device.getName().length() > 0)
		{
			viewHolder.deviceName.setText(device.getName());
		}

		//rssi
		String strRssiValue = mContext.getString(R.string.BEACON_RSSI_VALUE) + device.getAdvRssi();
		viewHolder.rssiState.setText(strRssiValue);

		//Tx power level
		/*
		String strTxPower =  mContext.getString(R.string.BEACON_TX_POWER) + device.getPower();
		viewHolder.deviceTxPower.setText(strTxPower);
		*/

		//mac address
		String strMacAddress =  mContext.getString(R.string.BEACON_MAC_ADDRESS) + device.getMacAddrEx();
		viewHolder.deviceMacAddr.setText(strMacAddress);

		//conn enable
		String strConnEnable =  mContext.getString(R.string.BEACON_CONN_ENABLE) + device.getAdvConnEnableString();
		viewHolder.deviceConnEnable.setText(strConnEnable);

		//rtt
		String strRtt =  mContext.getString(R.string.BEACON_RTT) + device.getRtt();
		viewHolder.deviceAdvRtt.setText(strRtt);

		//tempurture
		String strTemputure =  mContext.getString(R.string.BEACON_TEMP) + device.getAdvTemputure();
		viewHolder.deviceTempeture.setText(strTemputure);

		//battery level
		String strBatteryLvl =  mContext.getString(R.string.BEACON_BATTERY) + device.getAdvBattery();
		viewHolder.deviceBattery.setText(strBatteryLvl);

		//conn major
		String strMajorID =  mContext.getString(R.string.BEACON_MAJOR_LIST) + device.getMajorID();
		viewHolder.deviceMajor.setText(strMajorID);

		//conn major
		String strMinorID =  mContext.getString(R.string.BEACON_MINOR_LIST) + device.getAdvMinorID();
		viewHolder.deviceMinor.setText(strMinorID);

        //uuid
		if (device.getAdvBeaconType() == BeaconPerperal.BEACON_TYPE_EDDY_UID)
		{
			String strUid =  mContext.getString(R.string.EDDYSTONE_UID) + "0x" + device.getAdvEddystoneUID();
			viewHolder.deviceUUID.setText(strUid);
		}
		else if (device.getAdvBeaconType() == BeaconPerperal.BEACON_TYPE_EDDY_URL)
		{
			String strUrl =  mContext.getString(R.string.EDDYSTONE_URL_DESC) + device.getAdvEddystoneUrl();
			viewHolder.deviceUUID.setText(strUrl);
		}
		else if (device.getAdvBeaconType() == BeaconPerperal.BEACON_TYPE_IBEACON)
		{
			String strUuid =  mContext.getString(R.string.BEACON_UUID) +
					device.getAdvIBeaconUUID();
			viewHolder.deviceUUID.setText(strUuid);
		}

		return view;
	}

	class ViewHolder {
		TextView deviceName;      //名称
		TextView rssiState;     //状态

		//TextView deviceTxPower;

		TextView deviceBattery;

		TextView deviceMacAddr;

		TextView deviceConnEnable;

		TextView deviceAdvRtt;

		TextView deviceTempeture;

		TextView deviceMajor;

		TextView deviceMinor;

		TextView deviceUUID;
	}
}
