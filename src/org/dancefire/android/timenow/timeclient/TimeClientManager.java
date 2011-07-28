package org.dancefire.android.timenow.timeclient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.R;
import org.dancefire.android.timenow.TimePreference;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public abstract class TimeClientManager {
	private Context m_context;
	private ArrayList<TimeClient> m_clients = new ArrayList<TimeClient>();

	public TimeClientManager(Context context) {
		m_context = context;
	}

	public void start() {
		refresh();
	}

	public void stop() {
		for (TimeClient c : m_clients) {
			if (c.isRunning()) {
				c.stop();
			}
		}
	}

	public void refresh() {
		setClients();
		setClientStatus();
	}

	abstract protected void onTimeChanged(TimeResult result, TimeClient client);

	private void setClients() {
		setNtpClients();
		setGpsClients();
	}

	private boolean isWifiAvailable() {
		ConnectivityManager cm = (ConnectivityManager) m_context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
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

	private void setClientStatus() {
		boolean ntp_enabled = TimePreference.get().getBoolean(
				TimePreference.NTP_ENABLE, true);
		boolean gps_enabled = TimePreference.get().getBoolean(
				TimePreference.GPS_ENABLE, true);

		if (ntp_enabled) {
			// Check WIFI-only
			boolean ntp_wifi_only = TimePreference.get().getBoolean(
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

	private void setNtpClients() {
		String new_host = TimePreference.get().getString(
				TimePreference.NTP_SERVER,
				m_context.getString(R.string.pref_ntp_server_default));
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

	private void setGpsClients() {
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

}
