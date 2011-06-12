package org.dancefire.android.timenow;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public abstract class TimeClient implements Comparable<TimeClient>{
	public static final int TIME_NONE = 0;
	public static final int TIME_GPS = 1;
	public static final int TIME_NTP = 2;
	
	protected static final long INTERVAL_SHORT = 1000 * 5;
	protected static final long INTERVAL_LONG = 1000 * 60 * 60;
	
	protected static final int REPEATS = 10;

	protected static final long TIME_POINT;

	static {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		cal.set(2001, 1, 1, 1, 0, 0);
		TIME_POINT = cal.getTime().getTime();
	}

	protected int source = TIME_NONE;
	protected long diff = 0;
	protected long accuracy = Integer.MAX_VALUE;
	protected long lastUpdate = 0;
	protected long diff_sys_boot = Integer.MAX_VALUE;
	protected long interval = INTERVAL_SHORT;
	protected int count = 0;
	protected boolean running = false;

	protected Handler startHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			count = 0;
			start();
		};
	};
	
	public abstract void start();

	public abstract void stop();

	public abstract void onUpdated(int source, long diff, long accuracy);

	public void update(long diff, long accuracy) {
		
		if ((diff + System.currentTimeMillis()) > TIME_POINT) {
			//	If successfully received the time,
			if (this.accuracy >= accuracy) {
				this.diff = diff;
				this.accuracy = accuracy;
				this.lastUpdate = SystemClock.elapsedRealtime();
				this.diff_sys_boot = SystemClock.currentThreadTimeMillis() - SystemClock.elapsedRealtime();
				Log.i(Main.TAG, "TimeClient [" + source + "] = " + diff + " (" + accuracy + ")" + " [count = " + count + "]");
				onUpdated(source, diff, accuracy);
			} else {
				Log.i(Main.TAG, "TimeClient [" + source + "] is not more accurate than previous one. " + this.accuracy + " < " + accuracy + " [count = " + count + "]");
			}
			++count;
			//	if got enough accurate time, then slow down the update.
			if (count > REPEATS && interval == INTERVAL_SHORT) {
				Log.i(Main.TAG, "TimeClient [" + source + "] is slowing down.");
				stop();
				startHandler.sendEmptyMessageDelayed(0, INTERVAL_LONG);
			}
		} else {
			Log.w(Main.TAG, "TimeClient [" + source + "] received wrong time. " + diff);
		}

	}
	
	public int getSource() {
		return source;
	}
	
	public long getDifference() {
		return diff;
	}

	public long getAccuracy() {
		return accuracy;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}
	
	@Override
	public int compareTo(TimeClient another) {
		return (int)(this.accuracy - another.accuracy);
	}
}
