package org.dancefire.android.timenow.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.R;
import org.dancefire.android.timenow.TimePreference;
import org.dancefire.android.timenow.timeclient.GpsTimeClient;
import org.dancefire.android.timenow.timeclient.NtpTimeClient;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;
import org.dancefire.android.timenow.timeclient.Util;
import org.dancefire.android.timenow.timeclient.Util.DateFormatStyle;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class TimeService extends Service {
	public static final String TIME_STATUS_UPDATE_ACTION = "org.dancefire.android.action.TIME_STATUS_UPDATE_ACTION";
	public static final String TIME_UI_ACTION = "org.dancefire.android.action.TIME_UI_ACTION";
	public static final String TIME_TOAST_ACTION = "org.dancefire.android.action.TIME_TOAST_ACTION";
	public static final String UPDATE_ACTION = "action";
	public static final String IS_VISIBLE = "is_visible";
	public static final String SHOW_TOAST = "show_toast";

	private static final int REPEAT_ACTION = 0;
	private static final int TOAST_ACTION = 1;

	private static final int TOAST_LENGTH_SHORT = 1000;

	private ArrayList<TimeClient> m_clients = new ArrayList<TimeClient>();
	private BroadcastReceiver m_receiver_changed = null;
	private BroadcastReceiver m_receiver_ui = null;
	private BroadcastReceiver m_receiver_toast = null;
	private SharedPreferences m_pref = null;
	private Handler m_handler_repeat = null;
	private Handler m_handler_toast = null;
	private Toast m_toast = null;
	private TimeResult m_best_result = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		m_pref = PreferenceManager.getDefaultSharedPreferences(this);

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
					updateClientStatus();
				}
			}
		};

		// Toast handler
		m_handler_toast = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == TOAST_ACTION) {
					// repeat
					m_handler_toast.sendEmptyMessageDelayed(TOAST_ACTION,
							TOAST_LENGTH_SHORT - 100);
					showToast();
				}
			}
		};

		createToast();

		update();

		Log.d(Main.TAG, "Time Service is started.");
		super.onCreate();
	};

	@Override
	public void onDestroy() {
		for (TimeClient c : m_clients) {
			if (c.isRunning()) {
				c.stop();
			}
		}

		// Unregister Receiver
		unregisterReceiver(m_receiver_changed);
		unregisterReceiver(m_receiver_ui);
		unregisterReceiver(m_receiver_toast);

		m_handler_repeat.removeMessages(REPEAT_ACTION);
		m_handler_toast.removeMessages(TOAST_ACTION);

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

	private void update() {
		updateClients();
		updateClientStatus();
	}

	private void updateClients() {
		updateNtpClients();
		updateGpsClients();
	}

	private boolean isWifiAvailable() {
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info == null || cm.getBackgroundDataSetting()) {
			return false;
		}

		if (info.getType() == ConnectivityManager.TYPE_WIFI) {
			return info.isConnected();
		} else {
			return false;
		}
	}

	private void updateClientStatus() {
		boolean ntp_enabled = m_pref
				.getBoolean(TimePreference.NTP_ENABLE, true);
		boolean gps_enabled = m_pref
				.getBoolean(TimePreference.GPS_ENABLE, true);

		if (ntp_enabled) {
			// Check WIFI-only
			boolean ntp_wifi_only = m_pref.getBoolean(
					TimePreference.NTP_WIFI_ONLY, false);
			if (ntp_wifi_only) {
				ntp_enabled = isWifiAvailable();
			}
		}

		for (TimeClient c : m_clients) {
			// NTP
			if (c.getSourceType() == TimeClient.TIME_NTP) {
				if (ntp_enabled) {
					if (!c.isRunning()) {
						c.start();
					}
				} else {
					if (c.isRunning()) {
						c.stop();
					}
				}
			}
			// GPS
			if (c.getSourceType() == TimeClient.TIME_GPS) {
				if (gps_enabled) {
					if (!c.isRunning()) {
						c.start();
					}
				} else {
					if (c.isRunning()) {
						c.stop();
					}
				}
			}
		}
	}

	private void updateNtpClients() {
		String new_host = m_pref.getString(TimePreference.NTP_SERVER,
				this.getString(R.string.pref_ntp_server_default));
		ArrayList<TimeClient> new_clients = new ArrayList<TimeClient>();

		// Clean invalidate clients
		for (TimeClient item : m_clients) {
			if (item.getSourceType() == TimeClient.TIME_NTP) {
				NtpTimeClient ntc = (NtpTimeClient) item;
				if (ntc.getHost().equals(new_host)) {
					// put validate client to new list, ignore invalidate ones.
					new_clients.add(ntc);
				} else {
					// Stop client of old NTP server
					ntc.stop();
				}
			} else {
				// put other type of client to new list.
				new_clients.add(item);
			}
		}

		// Add more new clients if exist
		ArrayList<TimeClient> more_clients = new ArrayList<TimeClient>();
		try {
			InetAddress[] addresses = InetAddress.getAllByName(new_host);
			for (InetAddress addr : addresses) {
				boolean is_contained = false;
				// check whether contained already
				for (TimeClient item : new_clients) {
					if (item.getSourceType() == TimeClient.TIME_NTP) {
						NtpTimeClient ntc = (NtpTimeClient) item;
						if (ntc.getAddress().equals(addr.getHostAddress())) {
							is_contained = true;
						}
					}
				}
				if (!is_contained) {
					// not exist, add new client
					more_clients.add(new NtpTimeClient(addr) {
						@Override
						public void onUpdated(TimeResult result) {
							onTimeChanged(result, this);
						}
					});
				}
			}
		} catch (UnknownHostException e) {
			Log.e(Main.TAG, "Cannot resolve ntp server name: " + new_host);
		}
		// add new clients to final result
		new_clients.addAll(more_clients);
		m_clients = new_clients;
	}

	private void updateGpsClients() {
		boolean has_gps_client = false;
		for (TimeClient c : m_clients) {
			if (c.getSourceType() == TimeClient.TIME_GPS) {
				has_gps_client = true;
			}
		}
		if (!has_gps_client) {
			m_clients.add(new GpsTimeClient() {
				@Override
				public void onUpdated(TimeResult result) {
					onTimeChanged(result, this);
				}
			});
		}
	}

	private void setReceiver() {
		// Setup broadcast receiver of preference changed
		m_receiver_changed = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				update();
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
				update();
			}
		};
		// Setup broadcast receiver of Toast
		m_receiver_toast = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// start or stop toast
				boolean show_toast = intent.getBooleanExtra(SHOW_TOAST, true);
				if (show_toast) {
					m_handler_toast.sendEmptyMessage(TOAST_ACTION);
				} else {
					m_handler_toast.removeMessages(TOAST_ACTION);
				}
			}
		};

	}

	/* Toast */
	private void createToast() {
		m_toast = new Toast(this);
		m_toast.setDuration(Toast.LENGTH_SHORT);
		m_toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 10, 0);
		m_toast.setMargin(0.02f, 0.02f);
		LayoutInflater inflater = (LayoutInflater) (getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		View toast_view = inflater.inflate(R.layout.toast, null);
		m_toast.setView(toast_view);
	}

	private void showToast() {
		if (m_toast != null && m_best_result != null
				&& m_pref.getBoolean(TimePreference.TOAST_ENABLE, true)) {
			TextView tvMessage = (TextView) m_toast.getView().findViewById(
					R.id.toast_message);
			
			long diff = m_best_result.getLocalTimeError();
			long t = m_best_result.getCurrentSourceTime();
			String date = DateFormat.getDateInstance(DateFormat.LONG).format(t);
			String time = Util.formatDateTime(t, DateFormatStyle.TIME_ONLY);
			String offset = Util.getTimeSpanNumericString(diff);

			// Only show toast when the time error is larger than 30 seconds
			long thirty_seconds = 30 * Util.TIME_ONE_SECOND;
			if (Math.abs(diff) > thirty_seconds) {
				String fmt = getString(R.string.toast_message);
				tvMessage.setText(String.format(fmt, date, time, offset));
			} else {
				// Stop toast handler
				m_handler_toast.removeMessages(TOAST_ACTION);
				// Show new local time error
				String fmt = getString(R.string.toast_message_ok);
				tvMessage.setText(String.format(fmt, offset));
			}
			//m_toast.cancel();
			m_toast.show();
		}
	}
}
