## About
Map Alarmist is a simple location-based alarm app. It is distributed in the Google Play store: https://play.google.com/store/apps/details?id=com.vellut.geoalarm 
It is my first Android project and I used it to learn some aspects of the platform.

It includes examples of usage for the following:
- Google Maps (using the Google Play Services)
- Location Client (using the Google Play Services), with Location Updates and Location Geofencing
- Launching broadcast receiver at boot
- Using the Alarm Manager to wake up the device
- Swipe to delete in a ListView
- Preference screen
- Saving and reading to/from SharedPreferences
- Displaying the Alarm Picker dialog 

## Using the app
Use the map to select the area where the alarm should be triggered then select a sound and whether the phone should vibrate. Flip the switch at the bottom to set the alarm. Once inside the area, the phone will notify you. This application works even when another app is open or when the phone is sleeping.

Additional features include:
- Save preferred locations for quick retrieval
- Choose the location technique: Precise location using GPS, battery saving mode or a balanced accuracy/power technique.

## Compiling the app
Import the code into the Eclise ADT using the "Import... > Existing Android Code into Workspace" tool.

## Running
Generate a Google API key (used for Google Maps) for your debug and release signing keys. Replace the key inside the AndroidManifest.xml file.
Replace the AdMob tracking key inside the res/layout/activity_main.xml file
Create a Google Analytics configuration for the app and replace the tracking key inside the res/values/analytics.xml file


