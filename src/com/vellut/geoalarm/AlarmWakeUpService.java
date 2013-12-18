package com.vellut.geoalarm;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class AlarmWakeUpService extends Service implements ConnectionCallbacks,
		OnConnectionFailedListener, LocationListener {

	private LocationClient locationClient;
	private Intent wakefulIntent;
	private Looper serviceLooper;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		locationClient = new LocationClient(this, this, this);
		wakefulIntent = intent;

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		serviceLooper.quit();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		quitWakefulService();
	}

	@Override
	public void onConnected(Bundle bundle) {
		LocationRequest request = new LocationRequest();

		long timeout = 30 * 1000;
		HandlerThread thread = new HandlerThread("AlarmWakeUpServiceBackground");
		thread.start();
		serviceLooper = thread.getLooper();
		request.setNumUpdates(1).setFastestInterval(1).setInterval(1)
				.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
				.setExpirationDuration(timeout);
		locationClient.requestLocationUpdates(request, this, serviceLooper);

		// timeout for kill and free wake lock
		new CountDownTimer(timeout, timeout) {
			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				quitWakefulService();
			}
		}.start();
	}

	@Override
	public void onDisconnected() {
		quitWakefulService();
	}

	@Override
	public void onLocationChanged(Location location) {
		try {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			float accuracy = location.getAccuracy();
			
			Log.i(GeoAlarmUtils.APPTAG, "OnLocationChanged " + latitude + " " + longitude);

			// FIXME Check if circle intersects alarm area
			// current check only if lat/lon in alarm area
			GeoAlarm geoAlarm = new GeoAlarm();
			geoAlarm.restorePreferences(this);
			if (latitude >= geoAlarm.zone.southwest.latitude
					&& latitude <= geoAlarm.zone.northeast.latitude) {
				if (longitude >= geoAlarm.zone.southwest.longitude
						&& longitude <= geoAlarm.zone.northeast.longitude) {
					NotificationCompat.Builder builder = new NotificationCompat.Builder(
							this);
					
					Log.i(GeoAlarmUtils.APPTAG, "Alarm has fired");

					// Set the notification contents
					builder.setSmallIcon(R.drawable.ic_notification)
							.setContentTitle("Alarm Notif")
							.setContentText("Explicit Alarn has fired")
							.setAutoCancel(true);

					builder.setVibrate(new long[] { 0, 200, 1000, 200, 1000,
							200, 1000, 200, 1000 });

					// Get an instance of the Notification manager
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

					// Issue the notification
					mNotificationManager.notify(65712, builder.build());

				}
			}

			// If intersects Set shared Preferences to indicate that already
			// entered

			// and send notification; else do nothing

		} catch (Exception e) {
		} finally {
			quitWakefulService();
		}
	}

	private void quitWakefulService() {
		AlarmWakeUpBroadcastReceiver.completeWakefulIntent(wakefulIntent);
		stopSelf();
	}

}
