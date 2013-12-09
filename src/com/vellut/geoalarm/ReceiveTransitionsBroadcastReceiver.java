package com.vellut.geoalarm;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class ReceiveTransitionsBroadcastReceiver extends
		WakefulBroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		 Intent service = new Intent(context, ReceiveTransitionsIntentService.class);
		 Log.d(GeoAlarmUtils.APPTAG, "in ReceiveTransBrodcastReceiver");
		 service.putExtras(intent);
	     startWakefulService(context, service);
	}

}
