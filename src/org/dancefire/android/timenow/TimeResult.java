package org.dancefire.android.timenow;

import android.os.SystemClock;

public class TimeResult {
	public String id;
	public int source;
	public long phone_time;
	public long uptime;
	public long source_time;
	public long accuracy;
	
	public TimeResult() {
	}
	
	public TimeResult(int source) {
		this.source = source;
		this.phone_time = System.currentTimeMillis();
		this.uptime = SystemClock.elapsedRealtime();
	}
	
	public long getDifference() {
		long phone_to_source = phone_time - source_time;
		long phone_to_uptime = phone_time - uptime;
		if (isPhoneTimeChanged(phone_to_uptime)) {
			//	Get new difference between phone time and source time.
			long new_phone_to_uptime = getPhoneToUptime();
			long diff = new_phone_to_uptime - phone_to_uptime;
			phone_to_source = phone_to_source + diff;
		}
		return phone_to_source;
	}
	
	public long getCurrentTime() {
		long current_source_time = System.currentTimeMillis() - getDifference();
		return current_source_time;
	}
	
	public static boolean isPhoneTimeChanged(long old_phone_to_uptime) {
		long diff = Math.abs(getPhoneToUptime() - old_phone_to_uptime);
		if (diff < 50) {
			return false;
		} else {
			return true;
		}
	}
	
	public static long getPhoneToUptime() {
		return System.currentTimeMillis() - SystemClock.elapsedRealtime();
	}
	
	public static String getTimeSpanString(long timespan) {
		final long TIME_ONE_SECOND	= 1000;
		final long TIME_ONE_MINUTE	= TIME_ONE_SECOND * 60;
		final long TIME_ONE_HOUR	= TIME_ONE_MINUTE * 60;
		final long TIME_ONE_DAY	= TIME_ONE_HOUR * 24;
		final long TIME_ONE_WEEK	= TIME_ONE_DAY * 7;
		final long TIME_ONE_MONTH	= TIME_ONE_DAY * 30;
		final long TIME_ONE_YEAR	= TIME_ONE_DAY * 365;
		
		String postfix = "";
		String text;
		long abs_timespan = Math.abs(timespan);
		if (timespan < 0) {
			postfix = " ago";
		}
		
		if (abs_timespan < TIME_ONE_SECOND) {
			text = Long.toString(abs_timespan) + " ms";
		}else if(abs_timespan < TIME_ONE_MINUTE) {
			text = Long.toString(abs_timespan / TIME_ONE_SECOND) + " s";
		}else if(abs_timespan < TIME_ONE_HOUR) {
			text = Long.toString(abs_timespan / TIME_ONE_MINUTE) + " min";
		}else if(abs_timespan < TIME_ONE_DAY) {
			text = Long.toString(abs_timespan / TIME_ONE_HOUR) + " hours";
		}else if(abs_timespan < TIME_ONE_WEEK) {
			text = Long.toString(abs_timespan / TIME_ONE_DAY) + " days";
		}else if(abs_timespan < TIME_ONE_MONTH) {
			text = Long.toString(abs_timespan / TIME_ONE_WEEK) + " weeks";
		}else if(abs_timespan < TIME_ONE_YEAR) {
			text = Long.toString(abs_timespan / TIME_ONE_MONTH) + " months";
		}else{
			text = Long.toString(abs_timespan / TIME_ONE_YEAR) + " years";
		}
		
		return text + postfix;
	}
}
