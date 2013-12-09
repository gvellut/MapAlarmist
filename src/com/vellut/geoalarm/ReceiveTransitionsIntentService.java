package com.vellut.geoalarm;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class ReceiveTransitionsIntentService extends IntentService {

	public ReceiveTransitionsIntentService() {
		super("ReceiveTransitionsIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(GeoAlarmUtils.APPTAG, "ReceiveTransitionsItentService HandleIntent");

		// First check for errors
		if (LocationClient.hasError(intent)) {
			int errorCode = LocationClient.getErrorCode(intent);
			// Log the error
			Log.e(GeoAlarmUtils.APPTAG,
					getString(R.string.geofence_transition_error_detail,
							errorCode));
		} else {
			int transition = LocationClient.getGeofenceTransition(intent);
			// Test that a valid transition was reported
			if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
				Log.d(GeoAlarmUtils.APPTAG,"GeoAlarm triggered");
				boolean isUseVbirate = intent.getExtras().getBoolean(GeoAlarmUtils.EXTRA_USE_VIBRATE);
				String ringtoneUri = intent.getExtras().getString(GeoAlarmUtils.EXTRA_RINGTONE_URI);
				sendNotification(ringtoneUri, isUseVbirate);
			}
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
						getString(
								R.string.geofence_transition_notification_title))
				.setContentText(
						getString(R.string.geofence_transition_notification_text))
				.setContentIntent(notificationPendingIntent)
				.setAutoCancel(true);
		
		if(ringtoneUri != null) {
			builder.setSound(Uri.parse(ringtoneUri), Notification.STREAM_DEFAULT);
		}
		
		if(isUseVibrate) {
			builder.setVibrate(new long[]{0, 200, 1000, 200, 1000,200, 1000,200, 1000});
		}

		// Get an instance of the Notification manager
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Issue the notification
		mNotificationManager.notify(GeoAlarmUtils.GEOFENCE_NOTIFICATION_ID, builder.build());
	}

}
