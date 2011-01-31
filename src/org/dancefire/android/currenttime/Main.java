package org.dancefire.android.currenttime;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

public class Main extends Activity {
	private TextView textSystemTime = null;
	private TextView textNTPTime = null;
	private TextView textGPSTime = null;

	private long diff_ntp = 0;
	private long diff_gps = 0;

	private Handler handler = null;
	private BroadcastReceiver receiver = null;

	private OnTouchListener onTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int padding = v.getPaddingLeft();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				v.setBackgroundResource(R.drawable.clock_text_background_1);
				v.setPadding(padding, 0, padding, 0);
				break;
			case MotionEvent.ACTION_UP:
				v.setBackgroundResource(R.drawable.clock_text_background_2);
				v.setPadding(padding, 0, padding, 0);
				break;
			}

			return true;
		}
	};

	public static final SimpleDateFormat fmt = new SimpleDateFormat(
			"HH:mm:ss.SSS");
	public static final String TIME_UPDATE_ACTION = "org.dancefire.android.currenttime.time_update";
	public static final int SOURCE_SYS = 0;
	public static final int SOURCE_NTP = 1;
	public static final int SOURCE_GPS = 2;
	public static final String TAG = "CurrentTime";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// restore saved state
		if (savedInstanceState != null) {
			diff_ntp = savedInstanceState.getLong("diff_ntp");
			diff_gps = savedInstanceState.getLong("diff_gps");
		}
		// config about orientation
		setContentView(R.layout.main);
		
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
		case Surface.ROTATION_180:
			Log.i(Main.TAG, "SCREEN_ORIENTATION_PORTRAIT");
			break;
		case Surface.ROTATION_90:
		case Surface.ROTATION_270:
			Log.i(Main.TAG, "SCREEN_ORIENTATION_LANDSCAPE");
			break;
		default:
			Log.i(Main.TAG, "SCREEN_ORIENTATION = " + rotation);
			break;
		}
		// Get TextViews for times
		textSystemTime = (TextView) findViewById(R.id.SystemTime);
		textNTPTime = (TextView) findViewById(R.id.NTPTime);
		textGPSTime = (TextView) findViewById(R.id.GPSTime);
		// set ontouch listener
		textSystemTime.setOnTouchListener(this.onTouchListener);
		textNTPTime.setOnTouchListener(this.onTouchListener);
		textGPSTime.setOnTouchListener(this.onTouchListener);

		// Setup update handler
		handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				updateTime();
				handler.sendEmptyMessageDelayed(0, 30);
			};
		};
		// Setup broadcast receiver
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int source = intent.getIntExtra("source", 0);
				long diff = intent.getLongExtra("diff", 0);
				Log.i(Main.TAG, "Received broadcast from [" + source + "] = "
						+ diff);
				switch (source) {
				case SOURCE_NTP:
					diff_ntp = diff;
					break;
				case SOURCE_GPS:
					diff_gps = diff;
					break;
				}
				updateTime();
			}
		};
	}

	@Override
	protected void onResume() {
		// Start Service
		Intent intent;
		intent = new Intent();
		intent.setClass(Main.this, GPSTimeService.class);
		startService(intent);

		intent = new Intent();
		intent.setClass(Main.this, NTPTimeService.class);
		startService(intent);

		// Register Receiver
		registerReceiver(receiver, new IntentFilter(Main.TIME_UPDATE_ACTION));
		// Begin message loop
		handler.sendEmptyMessage(0);

		super.onResume();
	}

	@Override
	protected void onPause() {
		// Stop Service
		Intent intent;
		intent = new Intent();
		intent.setClass(Main.this, GPSTimeService.class);
		stopService(intent);
		Log.i(Main.TAG, "Stoping GPS service");

		intent = new Intent();
		intent.setClass(Main.this, NTPTimeService.class);
		stopService(intent);
		Log.i(Main.TAG, "Stoping NTP service");

		// Unregister Receiver
		unregisterReceiver(receiver);
		Log.i(Main.TAG, "Unregistering receiver");
		// stop message loop
		handler.removeMessages(0);

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(Main.TAG, "Saving instance state.");
		outState.putLong("diff_ntp", diff_ntp);
		outState.putLong("diff_gps", diff_gps);
		super.onSaveInstanceState(outState);
	}

	private void updateTime() {
		long sys_time = System.currentTimeMillis();
		textSystemTime.setText(fmt.format(sys_time));
		if (diff_ntp > 0)
			textNTPTime.setText(fmt.format(sys_time + diff_ntp));
		if (diff_gps > 0)
			textGPSTime.setText(fmt.format(sys_time + diff_gps));
	}
}