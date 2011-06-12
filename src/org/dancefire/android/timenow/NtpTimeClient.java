package org.dancefire.android.timenow;

import android.os.SystemClock;
import android.util.Log;

public abstract class NtpTimeClient extends TimeClient {
	private static final int NTP_TIMEOUT = 10000;

	private Thread thread = null;

	public NtpTimeClient() {
		this.source = TIME_NTP;
	}

	@Override
	public void start() {
		if (!running) {

			this.thread = new Thread() {
				@Override
				public void run() {
					Log.i(Main.TAG, "NTP Thread started.");
					SntpClient sntp = new SntpClient();
					while (running) {
						if (sntp.requestTime("pool.ntp.org", NTP_TIMEOUT)) {
							long sys_time = System.currentTimeMillis();
							long ntp_time = sntp.getNtpTime()
									+ SystemClock.elapsedRealtime()
									- sntp.getNtpTimeReference();
							;
							update(ntp_time - sys_time, sntp.getRoundTripTime());
						} else {
							Log.i(Main.TAG, "sntp.requestTime() returns fail");
						}
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					Log.i(Main.TAG, "NTP Thread stoped.");
				};
			};
			thread.start();
			running = true;
			Log.i(Main.TAG, "NTP Client started.");
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
			Log.i(Main.TAG, "NTP Client stopped.");
		}
	}

}
