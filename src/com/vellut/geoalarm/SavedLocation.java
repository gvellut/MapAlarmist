package com.vellut.geoalarm;

import com.google.android.gms.maps.model.LatLngBounds;

public class SavedLocation implements Comparable<SavedLocation> {
	public String description;
	public LatLngBounds zone;
	
	public SavedLocation() {
	}
	
	public SavedLocation(String description, LatLngBounds zone) {
		this.description = description;
		this.zone = zone;
	}
	
	@Override
	public String toString() {
		return description;
	}

	@Override
	public int compareTo(SavedLocation another) {
		return (this.description.compareTo(another.description));
	}
}
