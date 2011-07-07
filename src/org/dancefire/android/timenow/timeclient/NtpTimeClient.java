package org.dancefire.android.timenow.timeclient;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.dancefire.android.timenow.MainActivity;
import org.dancefire.android.timenow.util.SntpClient;

import android.os.SystemClock;
import android.util.Log;

public abstract class NtpTimeClient extends TimeClient {
	private static final int NTP_TIMEOUT = 10000;
	
	public static final String IP = "ntp_ip";
	public static final String NAME = "ntp_name";

	private Thread thread = null;
	private String host;

	public NtpTimeClient() {
		this.source = TIME_NTP;
		this.host = "pool.ntp.org";
	}

	public NtpTimeClient(String host) {
		this.source = TIME_NTP;
		this.host = host;
	}

	@Override
	public void start() {
		if (!running) {

			this.thread = new Thread() {
				@Override
				public void run() {
					Log.d(MainActivity.TAG, "NTP Thread [" + host + "] started.");
					while (running) {
						sntpRequest();
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					Log.d(MainActivity.TAG, "NTP Thread [" + host + "] stoped.");
				};
			};
			thread.start();
			running = true;
			Log.d(MainActivity.TAG, "NTP Client [" + host + "] started.");
		}
	}

	@Override
	public void stop() {
		if (running) {
			if (thread != null && thread.isAlive()) {
				thread.interrupt();
				try {
					Thread.sleep(INTERVAL_SHORT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			thread = null;
			running = false;
			Log.d(MainActivity.TAG, "NTP Client [" + host + "] stopped.");
		}
	}

	private void sntpRequest() {
		SntpClient sntp = new SntpClient();
		if (sntp.requestTime(this.host, NTP_TIMEOUT)) {
			long ntp_time = sntp.getNtpTime() + SystemClock.elapsedRealtime()
					- sntp.getNtpTimeReference();
			;

			InetAddress ip = sntp.getIpAddress();

			TimeResult result = createTimeResult(ip.getHostAddress(), ntp_time,
					sntp.getRoundTripTime());
			result.extra.putString(IP, ip.getHostAddress());
			try {
				result.extra.putString(NAME, InetAddress.getByAddress(
						ip.getAddress()).getHostName());
			} catch (UnknownHostException e) {
				result.extra.putString(NAME, host);
			}
			update(result);
		} else {
			Log.e(MainActivity.TAG, "sntp.requestTime(" + host + ") returns fail");
		}
	}

}
