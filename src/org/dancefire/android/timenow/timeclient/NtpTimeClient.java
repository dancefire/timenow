package org.dancefire.android.timenow.timeclient;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.util.SntpClient;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public abstract class NtpTimeClient extends TimeClient {
	private static final int NTP_TIMEOUT = 10000;
	private static final int NTP_ACTION = 1;

	public static final String IP = "ntp_ip";
	public static final String NAME = "ntp_name";

	private Handler m_handler = null;
	private String m_host;
	private String m_ip = null;

	public NtpTimeClient() {
		this.m_source = TIME_NTP;
		setHost("pool.ntp.org");
		setHandler();
	}

	public NtpTimeClient(String host) {
		this.m_source = TIME_NTP;
		setHost(host);
		setHandler();
	}

	public NtpTimeClient(InetAddress address) {
		this.m_source = TIME_NTP;
		this.m_host = address.getHostName();
		this.m_ip = address.getHostAddress();
		setHandler();
	}

	@Override
	protected void onStart() {
		m_handler.sendEmptyMessageDelayed(NTP_ACTION, m_interval);
		Log.d(Main.TAG, this + " is started.");
	}

	@Override
	protected void onStop() {
		m_handler.removeMessages(NTP_ACTION);
		Log.d(Main.TAG, this + " is stopped.");
	}

	@Override
	public String toString() {
		return "NTP Client [" + m_host + "/" + m_ip + "]";
	}

	public String getHost() {
		return this.m_host;
	}

	public String getAddress() {
		return this.m_ip;
	}

	private void setHost(String host) {
		this.m_host = host;
		try {
			this.m_ip = InetAddress.getByName(host).getHostAddress();
		} catch (UnknownHostException e) {
			Log.e(Main.TAG, this + " resolve name failed.");
		}
	}

	private void sntpRequest() {
		SntpClient sntp = new SntpClient();
		if (sntp.requestTime(this.m_ip, NTP_TIMEOUT)) {
			long ntp_time = sntp.getNtpTime() + SystemClock.elapsedRealtime()
					- sntp.getNtpTimeReference();
			;

			TimeResult result = createTimeResult(m_ip, ntp_time,
					sntp.getRoundTripTime());
			result.extra.putString(IP, m_ip);
			try {
				result.extra.putString(NAME, InetAddress.getByName(m_ip)
						.getHostName());
			} catch (UnknownHostException e) {
				result.extra.putString(NAME, m_host);
			}
			update(result);
		} else {
			Log.e(Main.TAG, "sntp.requestTime(" + m_host + ") returns fail");
		}
	}

	private void setHandler() {
		m_handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == NTP_ACTION) {
					if (m_running) {
						sntpRequest();
						m_handler.sendEmptyMessageDelayed(NTP_ACTION,
								m_interval);
					}
				}
				super.handleMessage(msg);
			}
		};
	}
}
