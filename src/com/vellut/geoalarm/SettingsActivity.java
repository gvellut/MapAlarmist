package com.vellut.geoalarm;

import com.google.analytics.tracking.android.EasyTracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private SettingsFragment settingsFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		settingsFragment = new SettingsFragment();
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, settingsFragment).commit();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}
	
	@Override 
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public static class SettingsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.settings);

			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				initSummary(getPreferenceScreen().getPreference(i));
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Preference pref = settingsFragment.findPreference(key);
		updateSummary(pref);
		
		GeoAlarmUtils.trackEvent(this, "ui_action", "button_press", "changed_location_technique",
				null);
	}

	private static void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updateSummary(p);
		}
	}

	private static void updateSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		}
		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
	}

}
