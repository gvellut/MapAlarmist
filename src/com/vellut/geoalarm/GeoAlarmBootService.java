package com.vellut.geoalarm;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;

public class GeoAlarmBootService extends Service implements
		ConnectionCallbacks, OnConnectionFailedListener,
		OnAddGeofencesResultListener {

	private GeoAlarm geoAlarm;
	private LocationClient locationClient;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		geoAlarm = new GeoAlarm();
		geoAlarm.restorePreferences(this);
		if (geoAlarm.isAlarmOn) {
			locationClient = new LocationClient(this, this, this);
			locationClient.connect();
		} else {
			stopSelf();
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// unsupported
		return null;
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		Log.d(GeoAlarmUtils.APPTAG, "Connected to Location Services in service");
		// setup alarm
		try {
			geoAlarm.setAlarm(this, locationClient, this);
		} catch (Exception e) {
			Log.e(GeoAlarmUtils.APPTAG, "Error setting alarm", e);
			
		}
	}

	@Override
	public void onDisconnected() {
		stopSelf();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		stopSelf();
	}

	@Override
	public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {

		Log.e(GeoAlarmUtils.APPTAG, "Connected to Location Services in service");
		stopSelf();
	}

}
