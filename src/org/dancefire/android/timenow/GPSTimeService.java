package org.dancefire.android.timenow;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class GPSTimeService extends Service {
	private static final long MIN_INTERVAL = 1000*60*10;
	private LocationManager manager = null;
	private LocationListener listener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			long diff = location.getTime() - System.currentTimeMillis();
			Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
			intent.putExtra("source", Main.SOURCE_GPS);
			intent.putExtra("diff", diff);
			sendBroadcast(intent);
		}

		@Override
		public void onProviderDisabled(String provider) {
			//Log.i(Main.TAG, "Provider Disabled: " + provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			//Log.i(Main.TAG, "Provider Enabled: " + provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			//Log.i(Main.TAG, "Status Changed: " + provider + "[" + status + "]");
		}
	};
	Handler handler = null;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		manager = (LocationManager) getSystemService(LOCATION_SERVICE);
		manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_INTERVAL, 0,
				listener);
		Log.i(Main.TAG, "GPS Service started.");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		manager.removeUpdates(listener);
		Log.i(Main.TAG, "GPS Service onDestroy()");
		super.onDestroy();
	}
}
