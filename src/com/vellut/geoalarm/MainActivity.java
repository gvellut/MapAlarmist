package com.vellut.geoalarm;

import java.util.Collections;

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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
		// FIXME test for wifi loc on

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
			geoAlarm.zone = getCurrentMapBounds();
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
		Switch onOffAlarm = (Switch) findViewById(R.id.switchOnOffAlarm);

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
						showMapLocation(geoAlarm.zone);
						Log.d(GeoAlarmUtils.APPTAG, "Showing zone saved in SharedPrefs");
						gMap.setOnCameraChangeListener(null);
					}
				});
			}

			// set vibrate mode
			CheckBox checkboxUseVibrate = (CheckBox) findViewById(R.id.checkboxUseVibrate);
			checkboxUseVibrate.setChecked(geoAlarm.isUseVibrate);

			// set toggle button state
			onOffAlarm.setChecked(geoAlarm.isAlarmOn);
		}

		if (geoAlarm.isAlarmOn) {
			disableUI();
		}

		// Events
		onOffAlarm
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						MainActivity.this.switchOnOffAlarm_onChange(buttonView,
								isChecked);
					}
				});
	}

	private void showMapLocation(LatLngBounds zone) {
		gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(zone, 0));
	}

	private void loadAd() {
		AdView adView = (AdView) this.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.addTestDevice("2EB20C5804AA87DD1C845CC0D2C79CF8").build();
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
		menu.findItem(R.id.action_load_location)
				.setEnabled(!geoAlarm.isAlarmOn);
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
		case R.id.action_load_location:
			showLoadLocationDialog();
			return true;
		case R.id.action_save_location:
			showSaveLocationDialog();
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

	private void showLoadLocationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_loadlocation, null);
		builder.setView(dialogView)
				.setTitle(getString(R.string.load_location_title))
				.setNegativeButton(R.string.cancel, null);
		final AlertDialog dialog = builder.create();

		final ArrayAdapter<SavedLocation> adapter = new ArrayAdapter<SavedLocation>(
				this, android.R.layout.simple_list_item_1, android.R.id.text1,
				geoAlarm.savedLocations);

		ListView listSavedLocations = (ListView) dialogView
				.findViewById(R.id.listSavedLocations);
		listSavedLocations.setAdapter(adapter);
		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
				listSavedLocations,
				new SwipeDismissListViewTouchListener.DismissCallbacks() {
					@Override
					public boolean canDismiss(int position) {
						return true;
					}

					@Override
					public void onDismiss(ListView listView,
							int[] reverseSortedPositions) {
						for (int position : reverseSortedPositions) {
							adapter.remove(adapter.getItem(position));
						}
						adapter.notifyDataSetChanged();
					}
				});
		listSavedLocations.setOnTouchListener(touchListener);
		listSavedLocations.setOnScrollListener(touchListener
				.makeScrollListener());
		listSavedLocations.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				showMapLocation(geoAlarm.savedLocations.get(position).zone);
				dialog.dismiss();
			}
		});
		
		showDialog(dialog);
	}

	private void showSaveLocationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();

		builder.setTitle(getString(R.string.save_location_title))
				.setView(inflater.inflate(R.layout.dialog_savelocation, null))
				.setPositiveButton(R.string.ok, null)
				.setNegativeButton(R.string.cancel, null);

		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			// Need to put listeners here since subviews are not ready before
			public void onShow(DialogInterface dialogi) {
				final AlertDialog dialog = (AlertDialog) dialogi;
				Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						saveLocation(dialog);
					}
				});

				EditText editTextLocationName = (EditText) dialog
						.findViewById(R.id.editTextLocationName);
				editTextLocationName
						.setOnEditorActionListener(new OnEditorActionListener() {
							public boolean onEditorAction(TextView v,
									int actionId, KeyEvent event) {
								if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
										|| (actionId == EditorInfo.IME_ACTION_DONE)) {
									return !saveLocation(dialog);
								}
								return false;
							}
						});
			}
		});

		showDialog(dialog);

	}

	private boolean saveLocation(AlertDialog dialog) {
		Log.d(GeoAlarmUtils.APPTAG, "Saving Location");
		EditText edit = (EditText) dialog
				.findViewById(R.id.editTextLocationName);
		String text = edit.getText().toString();
		if (!TextUtils.isEmpty(text)) {
			SavedLocation savedLocation = new SavedLocation(text,
					getCurrentMapBounds());
			MainActivity.this.geoAlarm.savedLocations.add(savedLocation);
			Collections.sort(MainActivity.this.geoAlarm.savedLocations);

			dialog.dismiss();
			Toast t = Toast.makeText(MainActivity.this,
					getString(R.string.location_saved_success),
					Toast.LENGTH_SHORT);
			t.show();
			return true;
		} else {
			// do not dismiss; show error toast at the top of
			// the screen
			Toast t = Toast.makeText(MainActivity.this,
					getString(R.string.bad_location_description),
					Toast.LENGTH_SHORT);
			t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 20);
			t.show();
			return false;
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

	public void switchOnOffAlarm_onChange(View v, boolean isChecked) {
		geoAlarm.isAlarmOn = isChecked;

		if (geoAlarm.isAlarmOn) {
			geoAlarm.zone = getCurrentMapBounds();
			geoAlarm.setAlarm(this, locationClient, this);
			geoAlarm.savePreferences(this);

			disableUI();
		} else {
			geoAlarm.disableAlarm(this, locationClient, this);
			geoAlarm.savePreferences(this);

			enableUI();
		}

		supportInvalidateOptionsMenu();

		trackEvent("ui_action", "button_press", "set_alarm", 1l);
	}

	private LatLngBounds getCurrentMapBounds() {
		return gMap.getProjection().getVisibleRegion().latLngBounds;
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

	// Define a DialogFragment that displays a dialog
	public static class GeoAlarmDialogFragment extends DialogFragment {
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public GeoAlarmDialogFragment() {
			super();
			mDialog = null;
		}

		@Override
		public void onCreate(Bundle bundle) {
			super.onCreate(bundle);
			setRetainInstance(true);
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
