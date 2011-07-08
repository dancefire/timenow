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

	private Thread thread = null;
	private String host;
	private String ip = null;

	public NtpTimeClient() {
		this.source = TIME_NTP;
		setHost("pool.ntp.org");
	}

	public NtpTimeClient(String host) {
		this.source = TIME_NTP;
		setHost(host);
	}

	public NtpTimeClient(InetAddress address) {
		this.source = TIME_NTP;
		this.host = address.getHostName();
		this.ip = address.getHostAddress();
	}

	@Override
	protected void onStart() {
		thread = new Thread() {
			@Override
			public void run() {
				while (running) {
					sntpRequest();
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						// interrupt
					}
				}
			};
		};
		thread.start();
		Log.d(Main.TAG, this + " is started.");
	}

	@Override
	protected void onStop() {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
			try {
				Thread.sleep(INTERVAL_SHORT);
			} catch (InterruptedException e) {
				// interrupt
			}
		}
		thread = null;
		Log.d(Main.TAG, this + " is stopped.");
	}
	
	@Override
	public String toString() {
		return "NTP Client [" + host + "/" + ip + "]";
	}
	
	private void setHost(String host) {
		this.host = host;
		try {
			this.ip = InetAddress.getByName(host).getHostAddress();
		} catch (UnknownHostException e) {
			Log.e(Main.TAG, this + " resolve name failed.");
		}
	}

	private void sntpRequest() {
		SntpClient sntp = new SntpClient();
		if (sntp.requestTime(this.ip, NTP_TIMEOUT)) {
			long ntp_time = sntp.getNtpTime() + SystemClock.elapsedRealtime()
					- sntp.getNtpTimeReference();
			;

			TimeResult result = createTimeResult(ip, ntp_time,
					sntp.getRoundTripTime());
			result.extra.putString(IP, ip);
			try {
				result.extra.putString(NAME, InetAddress.getByName(
						ip).getHostName());
			} catch (UnknownHostException e) {
				result.extra.putString(NAME, host);
			}
			update(result);
		} else {
			Log.e(Main.TAG, "sntp.requestTime(" + host + ") returns fail");
		}
	}

}
