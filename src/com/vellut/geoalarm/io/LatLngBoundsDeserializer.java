package com.vellut.geoalarm.io;

import java.lang.reflect.Type;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class LatLngBoundsDeserializer implements JsonDeserializer<LatLngBounds>{

	@Override
	public LatLngBounds deserialize(JsonElement element, Type arg1,
			JsonDeserializationContext arg2) throws JsonParseException {
		JsonArray arr = element.getAsJsonArray();
		LatLng southwest = new LatLng(arr.get(0).getAsDouble(), arr.get(1).getAsDouble());
		LatLng northeast = new LatLng(arr.get(2).getAsDouble(), arr.get(3).getAsDouble());
		LatLngBounds zone = new LatLngBounds(southwest, northeast);
		return zone;
	}

}
