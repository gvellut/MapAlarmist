package com.vellut.geoalarm;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	private final static int RINGTONE_PICKER_REQUEST_CODE = 1;

	private Uri ringtoneUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void buttonSetArea_onClick(View v) {
		Intent intent = new Intent(this, GeofenceActivity.class);
		startActivity(intent);
	}

	public void buttonSetRingtone_onClick(View v) {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
				"Select ringtone for alarm:");
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
				RingtoneManager.TYPE_ALARM);

		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
				ringtoneUri);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, ringtoneUri);
		startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RINGTONE_PICKER_REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				ringtoneUri = data
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (ringtoneUri != null) {
					Toast.makeText(this, ringtoneUri.toString(),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Silent", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		default:
			break;
		}
	}

}
