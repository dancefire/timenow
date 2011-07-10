package org.dancefire.android.timenow.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.R;
import org.dancefire.android.timenow.timeclient.GpsTimeClient;
import org.dancefire.android.timenow.timeclient.NtpTimeClient;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class TimeService extends Service {
	public static final String TIME_STATUS_UPDATE_ACTION = "org.dancefire.android.action.TIME_STATUS_UPDATE_ACTION";
	public static final String TIME_UI_ACTION = "org.dancefire.android.action.TIME_UI_ACTION";
	public static final String UPDATE_ACTION = "action";
	public static final String IS_VISIBLE = "is_visible";
	
	private static final int REPEAT_ACTION = 0;

	ArrayList<TimeClient> m_clients = new ArrayList<TimeClient>();
	BroadcastReceiver m_receiver_changed = null;
	BroadcastReceiver m_receiver_ui = null;
	String m_current_host = null;
	SharedPreferences m_pref = null;
	Handler m_handler_repeat = null;

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

		// Service handler
		m_handler_repeat = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == REPEAT_ACTION) {
					// start all clients
					updateClientStatus();
					// repeat
					m_handler_repeat.sendEmptyMessageDelayed(REPEAT_ACTION,
							TimeClient.INTERVAL_LONG);
				}
			}
		};

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

		Log.d(Main.TAG, "Time Service is stopped.");
		super.onDestroy();
	}

	private void onTimeChanged(TimeResult result, TimeClient client) {
		// send result
		Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
		intent.putExtras(result.toBundle());
		sendBroadcast(intent);
	}

	private void update() {
		updateClients();
		updateClientStatus();
	}

	private void updateClients() {
		updateNtpClients();
		updateGpsClients();
	}

	private void updateClientStatus() {
		boolean ntp_enabled = m_pref.getBoolean("pref_ntp_enable", true);
		boolean gps_enabled = m_pref.getBoolean("pref_gps_enable", true);

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
		String new_host = m_pref.getString("pref_ntp_server", this
				.getString(R.string.pref_ntp_server_default));
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
				boolean is_showed = intent.getBooleanExtra(IS_VISIBLE, true);
				if (is_showed) {
					m_handler_repeat.sendEmptyMessage(0);
				} else {
					m_handler_repeat.removeMessages(0);
				}
				update();
			}
		};
	}

}
