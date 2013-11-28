package com.vellut.geoalarm;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class MainActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

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
			// show Dialog to explain what to do

			// Actual zoom will be performed when location service
			// is connected
			zoomOnCurrentPosition = true;

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
			CheckBox checkboxUseVibrate = (CheckBox) 
					findViewById(R.id.checkboxUseVibrate);
			checkboxUseVibrate.setChecked(isUseVibrate);

			// set toggle button state
			ToggleButton togglebuttonOnOffAlarm = (ToggleButton) 
					findViewById(R.id.togglebuttonOnOffAlarm);
			togglebuttonOnOffAlarm.setChecked(isAlarmOn);

			if (isAlarmOn) {
				disableUI();
			}
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

		String ringtoneUri = settings.getString(PREF_RINGTONE_URI,
				RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
						.toString());
		if (ringtoneUri == null) {
			this.ringtoneUri = null;
		} else {
			this.ringtoneUri = Uri.parse(ringtoneUri);
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
		editor.putString(PREF_RINGTONE_URI, ringtoneUri.toString());
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
		if (servicesConnected()) {
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
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
						false);
		
		startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE);
	}

	public void togglebuttonOnOffAlarm_onClick(View v) {
		isAlarmOn = ((ToggleButton) v).isChecked();

		if (isAlarmOn) {
			// TODO start Alarm check
			
			disableUI();
		} else {
			// TODO stop alarm check
		
			enableUI();
		}
	}

	public void checkboxUseVibrate_onClick(View v) {
		isUseVibrate = ((CheckBox) v).isChecked();
	}
	
	private void disableUI() {
		// TODO disable map fragment
		findViewById(R.id.checkboxUseVibrate).setEnabled(false);
		findViewById(R.id.buttonSetRingtone).setEnabled(false);
	}
	
	private void enableUI() {
		findViewById(R.id.checkboxUseVibrate).setEnabled(true);
		findViewById(R.id.buttonSetRingtone).setEnabled(true);
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

	/**
	 * Google Play Services
	 */

	// Called by Location Services when the request to connect the client
	// finishes successfully.
	@Override
	public void onConnected(Bundle dataBundle) {
		if (zoomOnCurrentPosition) {
			zoomOnCurrentPosition();
			zoomOnCurrentPosition = false;
		}
	}

	// Called by Location Services if the connection to the location client
	// drops because of an error.
	@Override
	public void onDisconnected() {
		
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

	private boolean servicesConnected() {
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
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
					this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getSupportFragmentManager(),
						GeoAlarmUtils.APPTAG);
			}
			return false;
		}
	}

	private void showErrorDialog(int errorCode) {

		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();
			errorFragment.setDialog(errorDialog);
			errorFragment.show(getSupportFragmentManager(),
					GeoAlarmUtils.APPTAG);
		}
	}

	// Define a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
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
