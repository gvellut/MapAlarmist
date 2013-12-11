package com.vellut.geoalarm.io;

import java.lang.reflect.Type;


import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LatLngBoundsSerializer implements JsonSerializer<LatLngBounds> {

	@Override
	public JsonElement serialize(LatLngBounds zone, Type arg1,
			JsonSerializationContext arg2) {
		JsonArray arr = new JsonArray();
		arr.add(new JsonPrimitive(zone.southwest.latitude));
		arr.add(new JsonPrimitive(zone.southwest.longitude));
		arr.add(new JsonPrimitive(zone.northeast.latitude));
		arr.add(new JsonPrimitive(zone.northeast.longitude));
		return arr;
	}

}
