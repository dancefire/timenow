package org.dancefire.android.timenow.timeclient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.dancefire.android.timenow.R;
import org.dancefire.android.timenow.TimeApplication;

import android.text.format.DateUtils;

public final class Util {

	/* Formate Time Span */
	public static final long TIME_ONE_SECOND = 1000;
	public static final long TIME_ONE_MINUTE = TIME_ONE_SECOND * 60;
	public static final long TIME_ONE_HOUR = TIME_ONE_MINUTE * 60;
	public static final long TIME_ONE_DAY = TIME_ONE_HOUR * 24;
	public static final long TIME_ONE_WEEK = TIME_ONE_DAY * 7;
	public static final long TIME_ONE_MONTH = TIME_ONE_DAY * 30;
	public static final long TIME_ONE_YEAR = TIME_ONE_DAY * 365;

	private static final long[] TIME_BOUND = { 1, TIME_ONE_SECOND,
			TIME_ONE_MINUTE, TIME_ONE_HOUR, TIME_ONE_DAY, TIME_ONE_WEEK,
			TIME_ONE_MONTH, TIME_ONE_YEAR, Long.MAX_VALUE };

	private static final int[] TIME_ID = { R.plurals.timespan_microsecond,
			R.plurals.timespan_second, R.plurals.timespan_minute,
			R.plurals.timespan_hour, R.plurals.timespan_day,
			R.plurals.timespan_week, R.plurals.timespan_month,
			R.plurals.timespan_year };

	private static String getString(int id) {
		return TimeApplication.getAppContext().getResources().getString(id);
	}

	public static String getTimeSpanString(long timespan) {
		String prefix = "";
		String postfix = "";
		String text = "";
		long abs_timespan = Math.abs(timespan);

		if (timespan < 0) {
			prefix = getString(R.string.timespan_prefix_negative);
			postfix = getString(R.string.timespan_postfix_negative);
		} else {
			prefix = getString(R.string.timespan_prefix_positive);
			postfix = getString(R.string.timespan_postfix_positive);
		}

		for (int i = 1; i < TIME_BOUND.length; ++i) {
			if (abs_timespan < TIME_BOUND[i]) {
				int value = (int) (abs_timespan / TIME_BOUND[i - 1]);
				text = Integer.toString(value)
						+ " "
						+ TimeApplication.getAppContext().getResources()
								.getQuantityString(TIME_ID[i - 1], value);
				break;
			}
		}

		return prefix + " " + text + " " + postfix;
	}

	public static String getTimeSpanNumericString(long timespan) {
		StringBuilder sb = new StringBuilder();
		if (timespan < 0) {
			sb.append('-');
			timespan = -timespan;
		}
		sb.append(DateUtils.formatElapsedTime(timespan/1000));
		sb.append('.');
		sb.append(new DecimalFormat("000").format(timespan%1000));

		return sb.toString();
	}

	/* Format Date Time */
	private static final SimpleDateFormat[] DATE_FORMATS = {
			new SimpleDateFormat(), new SimpleDateFormat("HH:mm:ss"),
			new SimpleDateFormat("yyyy-MM-dd HH:mm"),
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
			};

	private static final int DATE_FORMAT_DEFAULT = 0;
	private static final int DATE_FORMAT_TIME_ONLY = 1;
	private static final int DATE_FORMAT_WITHOUT_SECOND = 2;
	private static final int DATE_FORMAT_FULL = 3;

	public enum DateFormatStyle {
		DEFAULT, TIME_ONLY, WITHOUT_SECOND, FULL
	};

	public static String formatDateTime(long time, DateFormatStyle style) {
		int index;
		switch (style) {
		case TIME_ONLY:
			index = DATE_FORMAT_TIME_ONLY;
			break;
		case WITHOUT_SECOND:
			index = DATE_FORMAT_WITHOUT_SECOND;
		case FULL:
			index = DATE_FORMAT_FULL;
			break;
		default:
			index = DATE_FORMAT_DEFAULT;
		}

		return DATE_FORMATS[index].format(time);
	}
	
	/* Validate Date */
	public static final long TIME_POINT;

	static {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		cal.set(2001, 1, 1, 1, 0, 0);
		TIME_POINT = cal.getTime().getTime();
	}



}
