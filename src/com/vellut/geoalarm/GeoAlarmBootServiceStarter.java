package com.vellut.geoalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeoAlarmBootServiceStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		 Intent i = new Intent();
	     i.setClass(context, GeoAlarmBootService.class);
	     context.startService(i);
	}

}
