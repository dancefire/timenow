package org.dancefire.android.timenow.timeclient;

import org.dancefire.android.timenow.MainActivity;

import android.os.SystemClock;
import android.util.Log;

public abstract class TimeClient {
	public static final int TIME_NONE = 0;
	public static final int TIME_GPS = 1;
	public static final int TIME_NTP = 2;

	protected static final long INTERVAL_SHORT = 1000 * 5;
	protected static final long INTERVAL_LONG = 1000 * 60 * 60;
	protected static final int REPEATS = 10;

	protected int source = TIME_NONE;
	protected long interval = INTERVAL_SHORT;
	protected boolean running = false;

	public abstract void start();

	public abstract void stop();

	public abstract void onUpdated(TimeResult result);

	public void update(TimeResult result) {
		result.local_time = System.currentTimeMillis();
		result.local_uptime = SystemClock.elapsedRealtime();

		if (result.getCurrentSourceTime() > Util.TIME_POINT) {
			// If successfully received the time,
			Log
					.d(MainActivity.TAG, "TimeClient [" + result.source + "] = "
							+ result.getLocalTimeError() + " ("
							+ result.accuracy + ")");
			onUpdated(result);
		} else {
			Log.e(MainActivity.TAG, "TimeClient [" + result.source
					+ "] received wrong time. " + result.getLocalTimeError());
		}

	}

	public int getSource() {
		return source;
	}

	public TimeResult createTimeResult(String id, long source_time,
			long accuracy) {
		TimeResult result = new TimeResult(source);
		result.id = id;
		result.source_time = source_time;
		result.accuracy = accuracy;
		return result;
	}
}
