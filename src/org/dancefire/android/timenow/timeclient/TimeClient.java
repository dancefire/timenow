package org.dancefire.android.timenow.timeclient;

import org.dancefire.android.timenow.Main;

import android.os.SystemClock;
import android.util.Log;

public abstract class TimeClient {
	public static final int TIME_NONE = 0;
	public static final int TIME_GPS = 1;
	public static final int TIME_NTP = 2;

	public static final long INTERVAL_SHORT = 1000 * 5;
	public static final long INTERVAL_LONG = 1000 * 60 * 60;
	protected static int batch_repeats = 10;

	protected int source = TIME_NONE;
	protected int count = 0;
	protected long interval = INTERVAL_SHORT;
	protected boolean running = false;

	protected abstract void onStart();

	protected abstract void onStop();

	protected abstract void onUpdated(TimeResult result);

	public void start() {
		if (!running) {
			count = 0;
			interval = INTERVAL_SHORT;
			onStart();
			running = true;
		}
	}

	public void stop() {
		if (running) {
			onStop();
			running = false;
		}
	}
	
	public boolean isRunning() {
		return running;
	}

	public void update(TimeResult result) {
		result.local_time = System.currentTimeMillis();
		result.local_uptime = SystemClock.elapsedRealtime();

		if (result.getCurrentSourceTime() > Util.TIME_POINT) {
			// If successfully received the time,
			Log
					.d(Main.TAG, "TimeClient [" + result.source + "] = "
							+ result.getLocalTimeError() + " ("
							+ result.accuracy + ")");
			++count;
			if (count >= batch_repeats) {
				stop();
			}
			onUpdated(result);
		} else {
			Log.e(Main.TAG, "TimeClient [" + result.source
					+ "] received wrong time. " + result.getLocalTimeError());
		}

	}

	public TimeResult createTimeResult(String id, long source_time,
			long accuracy) {
		TimeResult result = new TimeResult(source);
		result.id = id;
		result.source_time = source_time;
		result.accuracy = accuracy;
		return result;
	}
	
	public static void setRepeats(int repeats) {
		batch_repeats = repeats;
	}
	
	public static int getRepeats() {
		return batch_repeats;
	}
}
