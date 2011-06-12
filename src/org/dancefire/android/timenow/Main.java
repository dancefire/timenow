package org.dancefire.android.timenow;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	private TextView textSystemTime = null;
	private TextView textNTPTime = null;
	private TextView textGPSTime = null;

	private long diff_ntp = 0;
	private long diff_gps = 0;
	private static final int UPDATE_DELAY = 100;
	private static final int UPDATE_TOAST_DELAY = 2000;

	private Handler handler = null;
	private Handler toastHandler = null;
	private BroadcastReceiver receiver = null;

	private OnTouchListener onTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			View p = (View) (v.getParent());
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				p.setBackgroundColor(getResources().getColor(
						R.color.text_background_focused));
				break;
			case MotionEvent.ACTION_UP:
				p.setBackgroundColor(getResources().getColor(
						R.color.text_background_normal));
				showNotification();
				break;
			}

			return true;
		}
	};

	public static final SimpleDateFormat fmt = new SimpleDateFormat(
			"HH:mm:ss.SSS");
	public static final SimpleDateFormat fmtLong = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static final String TIME_UPDATE_ACTION = "org.dancefire.android.action.TIME_UPDATE";
	public static final int UPDATE_UI_ACTION = 0;
	public static final String TAG = "TimeNow";

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

		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		int rotation = display.getOrientation();
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
				if (msg.what == UPDATE_UI_ACTION) {
					updateTime();
					handler.sendEmptyMessageDelayed(UPDATE_UI_ACTION,
							UPDATE_DELAY);
				}
			};
		};

		toastHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == UPDATE_UI_ACTION) {
					if (diff_ntp > 1000 * 60) {
						String time = fmtLong.format(System.currentTimeMillis()
								+ diff_ntp);
						Toast.makeText(Main.this, time, Toast.LENGTH_SHORT)
								.show();

						showNotification();
					}
					toastHandler.sendEmptyMessageDelayed(UPDATE_UI_ACTION,
							UPDATE_TOAST_DELAY);
				}
				super.handleMessage(msg);
			}
		};

		// Setup broadcast receiver
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int source = intent.getIntExtra("source", TimeClient.TIME_NONE);
				long diff = intent.getLongExtra("diff", 0);
				Log.i(Main.TAG, "Received broadcast from [" + source + "] = "
						+ diff);
				switch (source) {
				case TimeClient.TIME_NTP:
					diff_ntp = diff;
					break;
				case TimeClient.TIME_GPS:
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
		startService(new Intent(Main.this, TimeService.class));

		// Register Receiver
		registerReceiver(receiver, new IntentFilter(Main.TIME_UPDATE_ACTION));

		// Begin message loop
		handler.sendEmptyMessage(UPDATE_UI_ACTION);
		toastHandler.sendEmptyMessage(UPDATE_UI_ACTION);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Stop Service
		stopService(new Intent(Main.this, TimeService.class));
		Log.i(Main.TAG, "Stoping Time service");

		// Unregister Receiver
		unregisterReceiver(receiver);
		Log.i(Main.TAG, "Unregistering receiver");

		// stop message loop
		handler.removeMessages(UPDATE_UI_ACTION);
		toastHandler.removeMessages(UPDATE_UI_ACTION);

		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(Main.TAG, "Saving instance state.");
		outState.putLong("diff_ntp", diff_ntp);
		outState.putLong("diff_gps", diff_gps);
		super.onSaveInstanceState(outState);
	}

	private void showNotification() {
		// Notification Bar
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		String timeStr = fmtLong.format(System.currentTimeMillis() + diff_ntp);

		int icon = R.drawable.icon;
		CharSequence tickerText = "Hello " + timeStr;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "My notification " + timeStr;
		CharSequence contentText = "Hello World! " + timeStr;
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				getApplicationContext(), 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, "", "", contentIntent);

		mNotificationManager.notify(1, notification);
	}

	private void updateTime() {
		long sys_time = System.currentTimeMillis();
		textSystemTime.setText(fmt.format(sys_time));
		if (diff_ntp != 0)
			textNTPTime.setText(fmt.format(sys_time + diff_ntp));
		if (diff_gps != 0)
			textGPSTime.setText(fmt.format(sys_time + diff_gps));
	}
}