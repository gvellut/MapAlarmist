package com.vellut.geoalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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
	GeoAlarm geoAlarm;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		locationClient = new LocationClient(this, this, this);
		locationClient.connect();
		wakefulIntent = intent;

		Log.i(GeoAlarmUtils.APPTAG, "In AlarmWakeUp Service");

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

		Log.i(GeoAlarmUtils.APPTAG, "In AlarmWakeUp Service: CONNECTED");

		geoAlarm = new GeoAlarm();
		geoAlarm.restorePreferences(this);
		int priority = geoAlarm.getLocationPriority(this);

		long timeout = 60 * 1000;
		HandlerThread thread = new HandlerThread("AlarmWakeUpServiceBackground");
		thread.start();
		serviceLooper = thread.getLooper();
		request.setNumUpdates(1).setFastestInterval(1).setInterval(1)
				.setPriority(priority).setExpirationDuration(timeout);
		locationClient.requestLocationUpdates(request, this, serviceLooper);

		// timeout for kill and free wake lock
		/*
		 * new CountDownTimer(timeout, timeout) { public void onTick(long
		 * millisUntilFinished) { }
		 * 
		 * public void onFinish() { Log.i(GeoAlarmUtils.APPTAG,
		 * "Timeout: No Location Fix"); quitWakefulService(); } }.start();
		 */
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

			// fIXME add a precision fuzz

			Log.i(GeoAlarmUtils.APPTAG, "OnLocationChanged " + latitude + " "
					+ longitude);

			if (latitude >= geoAlarm.zone.southwest.latitude
					&& latitude <= geoAlarm.zone.northeast.latitude
					&& longitude >= geoAlarm.zone.southwest.longitude
					&& longitude <= geoAlarm.zone.northeast.longitude) {

				if (!geoAlarm.isInZone) {
					// Only fire if not already in Zone
					Log.i(GeoAlarmUtils.APPTAG, "Alarm has fired: In Zone");

					geoAlarm.isInZone = true;
					geoAlarm.saveLocationZone(this);

					long alarmSetTime = geoAlarm.alarmSetTime;
					long currentTime = SystemClock.elapsedRealtime();
					if (currentTime - alarmSetTime > GeoAlarmUtils.MIN_DTIME) {
						String ringtoneUri = null;
						if (geoAlarm.ringtoneUri != null) {
							ringtoneUri = geoAlarm.ringtoneUri.toString();
						}
						boolean isUseVibrate = geoAlarm.isUseVibrate;
						sendNotification(ringtoneUri, isUseVibrate);
					}

				}
			} else {
				Log.i(GeoAlarmUtils.APPTAG, "Has Left Zone");
				geoAlarm.isInZone = false;
				geoAlarm.saveLocationZone(this);
			}

		} catch (Exception e) {
			Log.e(GeoAlarmUtils.APPTAG, "Error onlocationch", e);
		} finally {
			quitWakefulService();
		}
	}

	private void sendNotification(String ringtoneUri, boolean isUseVibrate) {

		// Create an explicit content Intent that starts the main Activity
		Intent notificationIntent = new Intent(getApplicationContext(),
				MainActivity.class);
		notificationIntent.setAction(GeoAlarmUtils.ACTION_STOP_ALARM);

		// Construct a task stack
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

		// Adds the main Activity to the task stack as the parent
		stackBuilder.addParentStack(MainActivity.class);

		// Push the content Intent onto the stack
		stackBuilder.addNextIntent(notificationIntent);

		// Get a PendingIntent containing the entire back stack
		PendingIntent notificationPendingIntent = stackBuilder
				.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get a notification builder that's compatible with platform versions
		// >= 4
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);

		// Set the notification contents
		builder.setSmallIcon(R.drawable.ic_notification)
				.setContentTitle(
						getString(R.string.geofence_transition_notification_title))
				.setContentText(
						getString(R.string.geofence_transition_notification_text))
				.setContentIntent(notificationPendingIntent)
				.setAutoCancel(true);

		if (ringtoneUri != null) {
			builder.setSound(Uri.parse(ringtoneUri),
					Notification.STREAM_DEFAULT);
		}

		if (isUseVibrate) {
			builder.setVibrate(new long[] { 0, 200, 1000, 200, 1000, 200, 1000,
					200, 1000 });
		}

		// Get an instance of the Notification manager
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Issue the notification
		mNotificationManager.notify(GeoAlarmUtils.GEOFENCE_NOTIFICATION_ID,
				builder.build());
	}

	private void quitWakefulService() {
		AlarmWakeUpBroadcastReceiver.completeWakefulIntent(wakefulIntent);
		stopSelf();
	}

}
