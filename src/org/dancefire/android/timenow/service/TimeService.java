package org.dancefire.android.timenow.service;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.SetTimeAssistant;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeClientManager;
import org.dancefire.android.timenow.timeclient.TimeResult;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class TimeService extends Service {
	public static final String TIME_STATUS_UPDATE_ACTION = "org.dancefire.android.action.TIME_STATUS_UPDATE_ACTION";
	public static final String TIME_UI_ACTION = "org.dancefire.android.action.TIME_UI_ACTION";
	public static final String TIME_TOAST_ACTION = "org.dancefire.android.action.TIME_TOAST_ACTION";
	public static final String UPDATE_ACTION = "action";
	public static final String IS_VISIBLE = "is_visible";
	public static final String SHOW_TOAST = "show_toast";

	private static final int REPEAT_ACTION = 0;

	private BroadcastReceiver m_receiver_changed = null;
	private BroadcastReceiver m_receiver_ui = null;
	private BroadcastReceiver m_receiver_toast = null;
	private Handler m_handler_repeat = null;
	private TimeResult m_best_result = null;
	private TimeClientManager m_client_manager = null;
	private SetTimeAssistant m_assistant = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		setReceiver();

		// Register Receiver
		registerReceiver(m_receiver_changed, new IntentFilter(
				TIME_STATUS_UPDATE_ACTION));
		registerReceiver(m_receiver_ui, new IntentFilter(TIME_UI_ACTION));
		registerReceiver(m_receiver_toast, new IntentFilter(TIME_TOAST_ACTION));

		// Service handler
		m_handler_repeat = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == REPEAT_ACTION) {
					// repeat
					m_handler_repeat.sendEmptyMessageDelayed(REPEAT_ACTION,
							TimeClient.INTERVAL_LONG);
					// start all clients
					m_client_manager.start();
				}
			}
		};

		// Set Time Client Manager
		m_client_manager = new TimeClientManager(this) {

			@Override
			protected void onTimeChanged(TimeResult result, TimeClient client) {
				TimeService.this.onTimeChanged(result, client);
			}
		};
		m_client_manager.refresh();

		// Set Time Assistant
		m_assistant = new SetTimeAssistant(this) {

			@Override
			protected TimeResult getTimeResult() {
				return m_best_result;
			}
		};

		Log.d(Main.TAG, "Time Service is started.");
		super.onCreate();
	};

	@Override
	public void onDestroy() {
		m_client_manager.stop();

		// Unregister Receiver
		unregisterReceiver(m_receiver_changed);
		unregisterReceiver(m_receiver_ui);
		unregisterReceiver(m_receiver_toast);

		m_handler_repeat.removeMessages(REPEAT_ACTION);
		m_assistant.stop();

		Log.d(Main.TAG, "Time Service is stopped.");
		super.onDestroy();
	}

	private void onTimeChanged(TimeResult result, TimeClient client) {
		// send result
		Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
		intent.putExtras(result.toBundle());
		sendBroadcast(intent);
		// save best result
		if (m_best_result == null) {
			m_best_result = result;
		} else {
			if (m_best_result.accuracy > result.accuracy) {
				m_best_result = result;
			}
		}
	}

	private void setReceiver() {
		// Setup broadcast receiver of preference changed
		m_receiver_changed = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				m_client_manager.refresh();
			}
		};
		// Setup broadcast receiver of UI showed
		m_receiver_ui = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean is_visible = intent.getBooleanExtra(IS_VISIBLE, true);
				if (is_visible) {
					m_handler_repeat.sendEmptyMessage(REPEAT_ACTION);
				} else {
					m_handler_repeat.removeMessages(REPEAT_ACTION);
				}
				m_client_manager.refresh();
			}
		};
		// Setup broadcast receiver of Toast
		m_receiver_toast = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// start or stop toast
				boolean show_toast = intent.getBooleanExtra(SHOW_TOAST, true);
				Log.v(Main.TAG, "TimeService.m_receiver_toast: " + show_toast);

				if (show_toast) {
					m_assistant.start();
				} else {
					m_assistant.stop();
				}
			}
		};

	}

}
