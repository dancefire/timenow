package org.dancefire.android.timenow;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class NTPTimeService extends Service {
	private Thread thread = null;
	private boolean running = false;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		thread = new Thread() {
			@Override
			public void run() {
				Log.i(Main.TAG, "NTP Thread started.");
				SntpClient sntp = new SntpClient();
				while (running) {
					if (sntp.requestTime("pool.ntp.org", 10000)) {
						long sys_time = System.currentTimeMillis();
						long ntp_time = sntp.getNtpTime() + SystemClock.elapsedRealtime() - sntp.getNtpTimeReference();;
						long diff = ntp_time - sys_time;
						Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
						intent.putExtra("source", Main.SOURCE_NTP);
						intent.putExtra("diff", diff);
						sendBroadcast(intent);
					}else{
						Log.i(Main.TAG, "sntp.requestTime() returns fail");
					}
					try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.i(Main.TAG, "NTP Thread stoped.");
			};
		};
		running = true;
		thread.start();
		Log.i(Main.TAG, "NTP Service started.");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}
		running = false;
		Log.i(Main.TAG, "NTP Service onDestroy()");
		super.onDestroy();
	}

}