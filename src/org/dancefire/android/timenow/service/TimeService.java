package org.dancefire.android.timenow.service;

import java.util.ArrayList;

import org.dancefire.android.timenow.Main;
import org.dancefire.android.timenow.timeclient.GpsTimeClient;
import org.dancefire.android.timenow.timeclient.NtpTimeClient;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TimeService extends Service {
	ArrayList<TimeClient> mList = new ArrayList<TimeClient>();
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		addTimeClient();
		for(int i = 0; i < mList.size(); ++i)
		{
			mList.get(i).start();
		}
		super.onCreate();
	};

	@Override
	public void onDestroy() {
		for(int i = 0; i < mList.size(); ++i)
		{
			mList.get(i).stop();
		}
		super.onDestroy();
	}
	
	private void addTimeClient() {
		//	Add NTP time client
		mList.add(new NtpTimeClient() {
			
			@Override
			public void onUpdated(TimeResult result) {
				onTimeChanged(result);
			}
		});
		//	Add GPS time client
		mList.add(new GpsTimeClient(this) {
			
			@Override
			public void onUpdated(TimeResult result) {
				onTimeChanged(result);
			}
		});
	}

//	private TimeClient getBestSource() {
//		TimeClient best = mList.get(0);
//		for (int i = 1; i < mList.size(); ++i) {
//			if (best.compareTo(mList.get(i)) > 0) {
//				best = mList.get(i);
//			}
//		}
//		return best;
//	}

	private void onTimeChanged(TimeResult result) {
		Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
		intent.putExtras(result.toBundle());
		sendBroadcast(intent);
	}

}
