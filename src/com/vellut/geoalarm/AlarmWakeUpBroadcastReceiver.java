package com.vellut.geoalarm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class AlarmWakeUpBroadcastReceiver extends WakefulBroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Do nothing
		Log.d(GeoAlarmUtils.APPTAG, "Received Alarm Notification");
		Intent launchService = new Intent();
		launchService.setClass(context, AlarmWakeUpService.class);
		startWakefulService(context, launchService);
	}

}
