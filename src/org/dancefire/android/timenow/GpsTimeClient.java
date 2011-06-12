package org.dancefire.android.timenow;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public abstract class GpsTimeClient extends TimeClient implements
		LocationListener {
	private static final long GPS_ACCURACY = 1000;

	private LocationManager locationManager = null;

	public GpsTimeClient(Context context) {
		this.source = TIME_GPS;
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public void onLocationChanged(Location location) {
		update(location.getTime() - System.currentTimeMillis(), GPS_ACCURACY);
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
					LocationManager.GPS_PROVIDER, interval, 0,
					this);
			running = true;
			Log.i(Main.TAG, "GPS Client started.");
		}
	}

	@Override
	public void stop() {
		if (running) {
			locationManager.removeUpdates(this);
			running = false;
			Log.i(Main.TAG, "GPS Client stopped.");
		}
	}
}
