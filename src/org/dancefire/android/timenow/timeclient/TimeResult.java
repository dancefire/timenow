package org.dancefire.android.timenow.timeclient;

import android.os.Bundle;
import android.os.SystemClock;

public class TimeResult implements Comparable<TimeResult>{
	public String id;
	public int source;
	public long local_time;
	public long local_uptime;
	public long source_time;
	public long accuracy;
	public Bundle extra = new Bundle();

	public static final String ID = "id";
	public static final String SOURCE = "source";
	public static final String LOCAL_TIME = "local_time";
	public static final String LOCAL_UPTIME = "local_uptime";
	public static final String SOURCE_TIME = "source_time";
	public static final String ACCURACY = "accuracy";
	public static final String EXTRA = "extra";

	public TimeResult() {
	}

	public TimeResult(int source) {
		this.source = source;
		this.local_time = System.currentTimeMillis();
		this.local_uptime = SystemClock.elapsedRealtime();
	}

	public long getLocalTimeError() {
		long diff = getCurrentSourceTime() - System.currentTimeMillis();
		return diff;
	}

	public long getCurrentSourceTime() {
		long diff = source_time - local_uptime;
		long current = SystemClock.elapsedRealtime() + diff;
		return current;
	}

	public Bundle toBundle() {
		Bundle b = new Bundle();
		b.putString(ID, this.id);
		b.putInt(SOURCE, this.source);
		b.putLong(LOCAL_TIME, this.local_time);
		b.putLong(LOCAL_UPTIME, this.local_uptime);
		b.putLong(SOURCE_TIME, this.source_time);
		b.putLong(ACCURACY, this.accuracy);
		b.putBundle(EXTRA, this.extra);
		return b;
	}

	@Override
	public boolean equals(Object o) {
		TimeResult obj = (TimeResult) o;
		return this.id.equals(obj.id);
	}

	public static TimeResult fromBundle(Bundle b) {
		TimeResult result = new TimeResult();

		result.id = b.getString(ID);
		result.source = b.getInt(SOURCE);
		result.local_time = b.getLong(LOCAL_TIME);
		result.local_uptime = b.getLong(LOCAL_UPTIME);
		result.source_time = b.getLong(SOURCE_TIME);
		result.accuracy = b.getLong(ACCURACY);
		result.extra = b.getBundle(EXTRA);

		return result;
	}

	@Override
	public int compareTo(TimeResult another) {
		return (int) (this.accuracy - another.accuracy);
	}

}
