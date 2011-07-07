package org.dancefire.android.timenow.timeclient;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.TimeApplication;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public abstract class GpsTimeClient extends TimeClient implements
		LocationListener {
	private static final long GPS_ACCURACY = 500;

	public static final String LONGITUDE = "gps_longitude";
	public static final String LATITUDE = "gps_latitude";
	public static final String ALTITUDE = "gps_altitude";
	public static final String ACCURACY = "gps_accuracy";

	private LocationManager locationManager = null;

	public GpsTimeClient() {
		this.source = TIME_GPS;
		locationManager = (LocationManager) TimeApplication.getAppContext()
				.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onLocationChanged(Location location) {
		TimeResult result = createTimeResult(LocationManager.GPS_PROVIDER,
				location.getTime(), GPS_ACCURACY
						+ (long) location.getAccuracy());
		result.extra.putDouble(LONGITUDE, location.getLongitude());
		result.extra.putDouble(LATITUDE, location.getLatitude());
		result.extra.putDouble(ALTITUDE, location.getAltitude());
		result.extra.putFloat(ACCURACY, location.getAccuracy());
		Log.v(Main.TAG, "GPS Location accuracy: " + location.getAccuracy());
		update(result);
	}

	@Override
	public void onProviderDisabled(String provider) {
		onStop();
	}

	@Override
	public void onProviderEnabled(String provider) {
		onStart();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	protected void onStart() {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				interval, 0, this);
		Log.d(Main.TAG, "GPS Client is started.");
	}

	@Override
	protected void onStop() {
		locationManager.removeUpdates(this);
		Log.d(Main.TAG, "GPS Client is stopped.");
	}
}
