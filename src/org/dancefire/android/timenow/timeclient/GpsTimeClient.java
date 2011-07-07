package org.dancefire.android.timenow.timeclient;

import org.dancefire.android.timenow.MainActivity;

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

	public GpsTimeClient(Context context) {
		this.source = TIME_GPS;
		locationManager = (LocationManager) context
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
		Log.v(MainActivity.TAG, "GPS Location accuracy: " + location.getAccuracy());
		update(result);
	}

	@Override
	public void onProviderDisabled(String provider) {
		stop();
	}

	@Override
	public void onProviderEnabled(String provider) {
		start();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void start() {
		if (!running) {
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, interval, 0, this);
			running = true;
			Log.d(MainActivity.TAG, "GPS Client is started.");
		}
	}

	@Override
	public void stop() {
		if (running) {
			locationManager.removeUpdates(this);
			running = false;
			Log.d(MainActivity.TAG, "GPS Client is stopped.");
		}
	}
}
