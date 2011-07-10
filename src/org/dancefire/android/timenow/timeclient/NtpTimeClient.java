package org.dancefire.android.timenow.timeclient;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.util.SntpClient;

import android.os.SystemClock;
import android.util.Log;

public abstract class NtpTimeClient extends TimeClient {
	private static final int NTP_TIMEOUT = 10000;

	public static final String IP = "ntp_ip";
	public static final String NAME = "ntp_name";

	private Thread m_thread = null;
	private String m_host;
	private String m_ip = null;

	public NtpTimeClient() {
		this.m_source = TIME_NTP;
		setHost("pool.ntp.org");
	}

	public NtpTimeClient(String host) {
		this.m_source = TIME_NTP;
		setHost(host);
	}

	public NtpTimeClient(InetAddress address) {
		this.m_source = TIME_NTP;
		this.m_host = address.getHostName();
		this.m_ip = address.getHostAddress();
	}
	
	@Override
	protected void onStart() {
		m_thread = new Thread() {
			@Override
			public void run() {
				while (m_running) {
					sntpRequest();
					try {
						Thread.sleep(m_interval);
					} catch (InterruptedException e) {
						// interrupt
					}
				}
			};
		};
		m_thread.start();
		Log.d(Main.TAG, this + " is started.");
	}

	@Override
	protected void onStop() {
		if (m_thread != null && m_thread.isAlive()) {
			m_thread.interrupt();
			try {
				Thread.sleep(INTERVAL_SHORT);
			} catch (InterruptedException e) {
				// interrupt
			}
		}
		m_thread = null;
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
				result.extra.putString(NAME, InetAddress.getByName(
						m_ip).getHostName());
			} catch (UnknownHostException e) {
				result.extra.putString(NAME, m_host);
			}
			update(result);
		} else {
			Log.e(Main.TAG, "sntp.requestTime(" + m_host + ") returns fail");
		}
	}

}
