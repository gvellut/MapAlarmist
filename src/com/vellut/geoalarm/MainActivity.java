package com.vellut.geoalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.media.RingtoneManager;
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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

	private boolean zoomOnCurrentPosition;
	private GeoAlarm geoAlarm;

	private LocationClient locationClient;

	// the zone in GeoAlarm used only at launch time to hold the zone
	// stored in Preferences
	// otherwise the zone is retrieved from the map when needed
	private GoogleMap gMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// FIXME add menu options Stop Alarm (needs notification id)
		// FIXME remove notification when clicking on Turn off Alarm
		// FIXME add time set for alarm + check dans
		// receivetransitionsintentservice
		// FIXME test for wifi loc on
		// Replace toggebutton with switch

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		locationClient = new LocationClient(this, this, this);
		gMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		disableNonScrollGestures();

		geoAlarm = new GeoAlarm();
		geoAlarm.restorePreferences(this);
		checkAlarm();
		initializeUI();
		loadAd();

		Log.d(GeoAlarmUtils.APPTAG, "CREATE");
	}

	@Override
	public void onStart() {
		super.onStart();

		locationClient.connect();

		Log.d(GeoAlarmUtils.APPTAG, "START");
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (!geoAlarm.isAlarmOn) { // already saved otherwise
			geoAlarm.zone = gMap.getProjection().getVisibleRegion().latLngBounds;
			geoAlarm.savePreferences(this);
		}

		Log.d(GeoAlarmUtils.APPTAG, "PAUSE");
	}

	@Override
	protected void onStop() {
		super.onStop();

		locationClient.disconnect();

		Log.d(GeoAlarmUtils.APPTAG, "STOP");
		EasyTracker.getInstance(this).activityStop(this);
	}

	private void initializeUI() {
		if (geoAlarm.isFirstTimeRun) {
			showWelcomeDialog();

			// Actual zoom will be performed when location service
			// is connected
			zoomOnCurrentPosition = true;

			geoAlarm.isFirstTimeRun = false;
		} else {
			if (geoAlarm.zone == null) {
				// Actual zoom will be performed when location service
				// is connected
				zoomOnCurrentPosition = true;
			} else {
				// Zoom on zone (last position)
				gMap.setOnCameraChangeListener(new OnCameraChangeListener() {
					@Override
					public void onCameraChange(CameraPosition pos) {
						gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
								geoAlarm.zone, 0));
						gMap.setOnCameraChangeListener(null);
					}
				});
			}

			// set vibrate mode
			CheckBox checkboxUseVibrate = (CheckBox) findViewById(R.id.checkboxUseVibrate);
			checkboxUseVibrate.setChecked(geoAlarm.isUseVibrate);

			// set toggle button state
			ToggleButton togglebuttonOnOffAlarm = (ToggleButton) findViewById(R.id.togglebuttonOnOffAlarm);
			togglebuttonOnOffAlarm.setChecked(geoAlarm.isAlarmOn);

			if (geoAlarm.isAlarmOn) {
				disableUI();
			}

		}
	}

	private void loadAd() {
		AdView adView = (AdView) this.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.addTestDevice("1CDF12C571A695FA0214F29BC40703DA").build();
		adView.loadAd(adRequest);
	}

	private void checkAlarm() {
		String action = getIntent().getAction();
		if (TextUtils.equals(action, GeoAlarmUtils.ACTION_STOP_ALARM)) {
			Log.d(GeoAlarmUtils.APPTAG, "StopAlarm");
			stopAlarm();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_current_location).setEnabled(
				!geoAlarm.isAlarmOn);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_current_location:
			zoomOnCurrentPosition();
			trackEvent("ui_action", "button_press", "zoom_on_current_position",
					null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void zoomOnCurrentPosition() {
		if (isGooglePlayServicesConnected()) {
			Location currentLocation = locationClient.getLastLocation();
			LatLng latLng = new LatLng(currentLocation.getLatitude(),
					currentLocation.getLongitude());
			float zoom = gMap.getCameraPosition().zoom;
			if (zoom < 14) {
				zoom = 14;
			}
			gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
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
				geoAlarm.ringtoneUri);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

		startActivityForResult(intent,
				GeoAlarmUtils.RINGTONE_PICKER_REQUEST_CODE);

		trackEvent("ui_action", "button_press", "set_ringtone", null);
	}

	public void togglebuttonOnOffAlarm_onClick(View v) {
		geoAlarm.isAlarmOn = ((ToggleButton) v).isChecked();

		if (geoAlarm.isAlarmOn) {
			geoAlarm.zone = gMap.getProjection().getVisibleRegion().latLngBounds;
			geoAlarm.savePreferences(this);
			geoAlarm.setAlarm(this, locationClient, this);

			disableUI();
		} else {
			geoAlarm.savePreferences(this);
			geoAlarm.disableAlarm(this, locationClient, this);

			enableUI();
		}

		supportInvalidateOptionsMenu();

		trackEvent("ui_action", "button_press", "set_alarm", 1l);
	}

	private void stopAlarm() {
		// Stop
		// nothing to do
	}

	public void checkboxUseVibrate_onClick(View v) {
		geoAlarm.isUseVibrate = ((CheckBox) v).isChecked();

		trackEvent("ui_action", "button_press", "use_vibrate", null);
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
		case GeoAlarmUtils.RINGTONE_PICKER_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				geoAlarm.ringtoneUri = data
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			}
			break;
		case GeoAlarmUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:
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
		Log.d(GeoAlarmUtils.APPTAG, "Connected to Location Services");

		if (zoomOnCurrentPosition) {
			zoomOnCurrentPosition();
			zoomOnCurrentPosition = false;
		}

		// FIXME alarm rings when the app opens inside the fence
		/*
		 * if (geoAlarm.isAlarmOn) { try { // set alarm geofence if needed (in
		 * case user removed it using // Force Stop) geoAlarm.setAlarm(this,
		 * locationClient, this); } catch (Exception e) {
		 * Log.e(GeoAlarmUtils.APPTAG, "Error setting alarm on start", e); } }
		 */
	}

	// Called by Location Services if the connection to the location client
	// drops because of an error.
	@Override
	public void onDisconnected() {
		Log.d(GeoAlarmUtils.APPTAG, "Disconnected from Location Services");
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						GeoAlarmUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
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
				this, GeoAlarmUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
		showDialog(errorDialog);
	}

	private void showWelcomeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.welcome).setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
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

	// Analytics

	private void trackEvent(String category, String action, String label,
			Long value) {
		EasyTracker easyTracker = EasyTracker.getInstance(this);
		easyTracker.send(MapBuilder.createEvent(category, action, label, value)
				.build());
	}
}
