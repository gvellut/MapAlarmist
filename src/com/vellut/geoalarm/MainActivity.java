package com.vellut.geoalarm;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MainActivity extends FragmentActivity implements
		ConnectionCallbacks, OnConnectionFailedListener,
		OnAddGeofencesResultListener, OnRemoveGeofencesResultListener {

	private final static int RINGTONE_PICKER_REQUEST_CODE = 1;
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 2;
	private final static String PREFERENCES_FILE_NAME = "com.vellut.geoalarm.Preferences";
	private final static String PREF_IS_FIRST_TIME_RUN = "isFirstTimeRun";
	private final static String PREF_RINGTONE_URI = "ringtoneUri";
	private final static String PREF_NORTH_LAT = "northLat";
	private final static String PREF_WEST_LON = "westLon";
	private final static String PREF_SOUTH_LAT = "southLat";
	private final static String PREF_EAST_LON = "eastLon";
	private final static String PREF_USE_VIBRATE = "useVibrate";
	private final static String PREF_IS_ALARM_ON = "isAlarmOn";

	private boolean isFirstTimeRun;
	private boolean zoomOnCurrentPosition;
	private boolean isAlarmOn;

	private Uri ringtoneUri;
	// used only at launch time to hold the zone stored in Preferences
	// otherwise the zone is retrieved from the map
	private LatLngBounds zone;
	private boolean isUseVibrate;

	private LocationClient locationClient;

	private PendingIntent transitionPendingIntent;

	// Note that this may be null if the Google Play services APK is not
	// available.
	private GoogleMap gMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		locationClient = new LocationClient(this, this, this);
		gMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		disableNonScrollGestures();

		checkAlarm();
		restorePreferences();
		initializeUI();

		Log.d(GeoAlarmUtils.APPTAG, "CREATE");
	}

	@Override
	public void onStart() {
		super.onStart();

		locationClient.connect();

		Log.d(GeoAlarmUtils.APPTAG, "START");
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(GeoAlarmUtils.APPTAG, "RESUME");
	}

	@Override
	protected void onPause() {
		super.onPause();

		savePreferences();

		Log.d(GeoAlarmUtils.APPTAG, "PAUSE");
	}

	@Override
	protected void onStop() {
		super.onStop();

		locationClient.disconnect();

		Log.d(GeoAlarmUtils.APPTAG, "STOP");
	}

	private void initializeUI() {
		if (isFirstTimeRun) {
			// TODO show Dialog to explain what to do

			// Actual zoom will be performed when location service
			// is connected
			zoomOnCurrentPosition = true;
			
			showWelcomeDialog();

			isFirstTimeRun = false;
		} else {
			if (zone == null) {
				// Actual zoom will be performed when location service
				// is connected
				zoomOnCurrentPosition = true;
			} else {
				// Zoom on zone (last position)
				gMap.setOnCameraChangeListener(new OnCameraChangeListener() {
					@Override
					public void onCameraChange(CameraPosition pos) {
						gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
								zone, 0));
						gMap.setOnCameraChangeListener(null);
					}
				});
			}

			// set vibrate mode
			CheckBox checkboxUseVibrate = (CheckBox) findViewById(R.id.checkboxUseVibrate);
			checkboxUseVibrate.setChecked(isUseVibrate);

			// set toggle button state
			ToggleButton togglebuttonOnOffAlarm = (ToggleButton) findViewById(R.id.togglebuttonOnOffAlarm);
			togglebuttonOnOffAlarm.setChecked(isAlarmOn);

			if (isAlarmOn) {
				disableUI();
			}
		}
	}

	private void checkAlarm() {
		String action = getIntent().getAction();
		if (TextUtils.equals(action, GeoAlarmUtils.ACTION_STOP_ALARM)) {
			Log.d(GeoAlarmUtils.APPTAG, "StopAlarm");
			stopAlarm();
		}
	}

	private void restorePreferences() {
		SharedPreferences settings = getSharedPreferences(
				PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

		this.isFirstTimeRun = settings.getBoolean(PREF_IS_FIRST_TIME_RUN, true);

		if (!isFirstTimeRun) {
			Log.d(GeoAlarmUtils.APPTAG, "Not First Time Run");
			double north = getDouble(settings.getString(PREF_NORTH_LAT, null));
			double south = getDouble(settings.getString(PREF_SOUTH_LAT, null));
			double west = getDouble(settings.getString(PREF_WEST_LON, null));
			double east = getDouble(settings.getString(PREF_EAST_LON, null));
			this.zone = new LatLngBounds(new LatLng(south, west), new LatLng(
					north, east));
		}

		if (isFirstTimeRun) {
			// Default alarm sound
			Uri defaultUri = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_ALARM);
			if (defaultUri == null) {
				defaultUri = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				if (defaultUri == null) {
					defaultUri = RingtoneManager
							.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
				}
			}
			this.ringtoneUri = defaultUri;
		} else {
			String sRingtoneUri = settings.getString(PREF_RINGTONE_URI, null);
			if (sRingtoneUri == null) {
				this.ringtoneUri = null;
			} else {
				this.ringtoneUri = Uri.parse(sRingtoneUri);
			}
		}

		this.isUseVibrate = settings.getBoolean(PREF_USE_VIBRATE, false);

		this.isAlarmOn = settings.getBoolean(PREF_IS_ALARM_ON, false);
	}

	private double getDouble(String sDouble) {
		if (sDouble == null) {
			return Double.NaN;
		} else {
			return Double.parseDouble(sDouble);
		}
	}

	private void savePreferences() {
		SharedPreferences settings = getSharedPreferences(
				PREFERENCES_FILE_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putBoolean(PREF_IS_FIRST_TIME_RUN, isFirstTimeRun);
		LatLngBounds zone = gMap.getProjection().getVisibleRegion().latLngBounds;
		editor.putString(PREF_NORTH_LAT,
				Double.toString(zone.northeast.latitude));
		editor.putString(PREF_SOUTH_LAT,
				Double.toString(zone.southwest.latitude));
		editor.putString(PREF_WEST_LON,
				Double.toString(zone.southwest.longitude));
		editor.putString(PREF_EAST_LON,
				Double.toString(zone.northeast.longitude));
		if (ringtoneUri != null) {
			editor.putString(PREF_RINGTONE_URI, ringtoneUri.toString());
		} else {
			editor.putString(PREF_RINGTONE_URI, null);
		}
		editor.putBoolean(PREF_USE_VIBRATE, isUseVibrate);
		editor.putBoolean(PREF_IS_ALARM_ON, isAlarmOn);

		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_current_location:
			zoomOnCurrentPosition();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void zoomOnCurrentPosition() {
		// If Google Play Services is available
		if (isGooglePlayServicesConnected()) {
			Location currentLocation = locationClient.getLastLocation();
			LatLng latLng = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
		}
	}

	public void buttonSetRingtone_onClick(View v) {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
				getString(R.string.ringtone_picker_title));
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_ALARM);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
				ringtoneUri);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

		startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE);
	}

	public void togglebuttonOnOffAlarm_onClick(View v) {
		isAlarmOn = ((ToggleButton) v).isChecked();

		if (isAlarmOn) {
			if (!isGooglePlayServicesConnected()) {
				return;
			}

			transitionPendingIntent = getTransitionPendingIntent();
			List<Geofence> geofences = new ArrayList<Geofence>();
			geofences.add(buildGeofence());
			locationClient.addGeofences(geofences, transitionPendingIntent,
					this);

			disableUI();
		} else {
			List<String> geofenceIds = new ArrayList<String>();
			geofenceIds.add(GeoAlarmUtils.GEOFENCE_REQUEST_ID);
			locationClient.removeGeofences(geofenceIds, this);
			transitionPendingIntent = null;

			enableUI();
		}
	}

	private Geofence buildGeofence() {
		LatLngBounds latLngBounds = gMap.getProjection().getVisibleRegion().latLngBounds;
		LatLng center = latLngBounds.getCenter();
		double dLng = Math.abs(latLngBounds.northeast.longitude
				- latLngBounds.southwest.longitude);
		double dLat = Math.abs(latLngBounds.northeast.latitude
				- latLngBounds.southwest.latitude);
		float radius = (float) Math.min(dLng, dLat);
		radius *= 111320; // Length of a degree in meter at equator
		return new Geofence.Builder()
				.setRequestId(GeoAlarmUtils.GEOFENCE_REQUEST_ID)
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
				.setExpirationDuration(Long.MAX_VALUE)
				.setCircularRegion(center.latitude, center.longitude, radius)
				.build();

	}

	private void stopAlarm() {
		// Stop
		// nothing to do
	}

	public void checkboxUseVibrate_onClick(View v) {
		isUseVibrate = ((CheckBox) v).isChecked();
	}

	private void disableUI() {
		// TODO disable map fragment
		gMap.getUiSettings().setAllGesturesEnabled(false);
		findViewById(R.id.checkboxUseVibrate).setEnabled(false);
		findViewById(R.id.buttonSetRingtone).setEnabled(false);
	}

	private void enableUI() {
		findViewById(R.id.checkboxUseVibrate).setEnabled(true);
		findViewById(R.id.buttonSetRingtone).setEnabled(true);
		gMap.getUiSettings().setAllGesturesEnabled(true);
		disableNonScrollGestures();
	}

	private void disableNonScrollGestures() {
		gMap.getUiSettings().setRotateGesturesEnabled(false);
		gMap.getUiSettings().setTiltGesturesEnabled(false);
		gMap.getUiSettings().setCompassEnabled(false);
		gMap.getUiSettings().setZoomControlsEnabled(false);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RINGTONE_PICKER_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				ringtoneUri = data
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			}
			break;
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			if (resultCode == RESULT_OK) {
				Log.d(GeoAlarmUtils.APPTAG, "PlayServices Resolved");
				break;
			} else {
				Log.d(GeoAlarmUtils.APPTAG, "PlayServices Not Resolved");
				break;
			}
		default:
			break;
		}
	}

	private PendingIntent getTransitionPendingIntent() {
		Intent intent = new Intent(this, ReceiveTransitionsIntentService.class);
		if (ringtoneUri != null) {
			intent.putExtra(GeoAlarmUtils.EXTRA_RINGTONE_URI,
					ringtoneUri.toString());
		}
		intent.putExtra(GeoAlarmUtils.EXTRA_USE_VIBRATE, isUseVibrate);

		return PendingIntent.getService(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	/*
	 * Handle the result of adding the geofences
	 */
	@Override
	public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
		Log.d(GeoAlarmUtils.APPTAG, "AddGeofencesResult");
		if (statusCode == LocationStatusCodes.SUCCESS) {
			Log.d(GeoAlarmUtils.APPTAG, "AddGeofencesResult Success!!!");
		} else {
			Log.d(GeoAlarmUtils.APPTAG, "AddGeofencesResult Error!!!");
		}
	}

	@Override
	public void onRemoveGeofencesByPendingIntentResult(int statusCode,
			PendingIntent transitionPendingIntent) {
		// never called
	}

	@Override
	public void onRemoveGeofencesByRequestIdsResult(int statusCode,
			String[] geofenceIds) {
		Log.d(GeoAlarmUtils.APPTAG, "RemoveGeofencesByRequestIdsResult");
		if (statusCode == LocationStatusCodes.SUCCESS) {
			Log.d(GeoAlarmUtils.APPTAG,
					"RemoveGeofencesByRequestIdsResult Success!!!");
		} else {
			Log.d(GeoAlarmUtils.APPTAG,
					"RemoveGeofencesByRequestIdsResult Error!!!");
		}
	}

	/**
	 * Google Play Services
	 */

	// Called by Location Services when the request to connect the client
	// finishes successfully.
	@Override
	public void onConnected(Bundle dataBundle) {
		Log.e(GeoAlarmUtils.APPTAG, "Connected to Location Services");

		if (zoomOnCurrentPosition) {
			zoomOnCurrentPosition();
			zoomOnCurrentPosition = false;
		}
	}

	// Called by Location Services if the connection to the location client
	// drops because of an error.
	@Override
	public void onDisconnected() {
		Log.e(GeoAlarmUtils.APPTAG, "Disconnected from Location Services");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			// If no resolution is available, display a dialog to the user with
			// the error.
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	private boolean isGooglePlayServicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			Log.d(GeoAlarmUtils.APPTAG, "Google Play services is available.");
			return true;
		} else {
			// Google Play services was not available for some reason
			// Display an error dialog
			showErrorDialog(resultCode);
			return false;
		}
	}

	private void showErrorDialog(int errorCode) {
		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
		showDialog(errorDialog);
	}
	
	private void showWelcomeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.welcome)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       
                   }
               });
        // Create the AlertDialog object and return it
        showDialog(builder.create());
	}
	
	private void showDialog(Dialog errorDialog) {
		if (errorDialog != null) {
			GeoAlarmDialogFragment errorFragment = new GeoAlarmDialogFragment();
			errorFragment.setDialog(errorDialog);
			errorFragment.show(getSupportFragmentManager(),
					GeoAlarmUtils.APPTAG);
		}
	}

	// Define a DialogFragment that displays the error dialog
	public static class GeoAlarmDialogFragment extends DialogFragment {
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public GeoAlarmDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}
}
