package org.dancefire.android.timenow.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.timeclient.GpsTimeClient;
import org.dancefire.android.timenow.timeclient.NtpTimeClient;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimeService extends Service {
	ArrayList<TimeClient> mList = new ArrayList<TimeClient>();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		addClients();
		for (int i = 0; i < mList.size(); ++i) {
			mList.get(i).start();
		}
		Log.d(Main.TAG, "Time Service is started.");
		super.onCreate();
	};

	@Override
	public void onDestroy() {
		for (int i = 0; i < mList.size(); ++i) {
			TimeClient t = mList.get(i);
			if (t.isRunning()) {
				t.stop();
			}
		}
		Log.d(Main.TAG, "Time Service is stopped.");
		super.onDestroy();
	}

	private void addClients() {
		// Add NTP time client
		String host = "pool.ntp.org";
		try {
			InetAddress[] list = InetAddress.getAllByName("pool.ntp.org");
			for (int i = 0; i < list.length; ++i) {
				mList.add(new NtpTimeClient(list[i]) {
					@Override
					public void onUpdated(TimeResult result) {
						onTimeChanged(result, this);
					}
				});
			}
		} catch (UnknownHostException e) {
			Log.e(Main.TAG, "Time  client [" + host + "] resolve name failed.");
		}
		// Add GPS time client
		mList.add(new GpsTimeClient() {
			@Override
			public void onUpdated(TimeResult result) {
				onTimeChanged(result, this);
			}
		});
	}

	private boolean isAllStopped() {
		boolean all_stopped = true;
		for (int i = 0; i < mList.size(); ++i) {
			if (mList.get(i).isRunning()) {
				all_stopped = false;
				break;
			}
		}
		return all_stopped;
	}

	private void onTimeChanged(TimeResult result, TimeClient client) {
		// send result
		Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
		intent.putExtras(result.toBundle());
		sendBroadcast(intent);
		// if all client stopped, then stop the service.
		if (isAllStopped()) {
			stopSelf();
		}
	}

}
