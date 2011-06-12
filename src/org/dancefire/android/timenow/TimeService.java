package org.dancefire.android.timenow;

import java.util.ArrayList;
import java.util.List;

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
			public void onUpdated(int source, long diff, long accuracy) {
				onTimeChanged(source);
			}
		});
		//	Add GPS time client
		mList.add(new GpsTimeClient(this) {
			
			@Override
			public void onUpdated(int source, long diff, long accuracy) {
				onTimeChanged(source);
			}
		});
	}

	private TimeClient getBestSource() {
		TimeClient best = mList.get(0);
		for (int i = 1; i < mList.size(); ++i) {
			if (best.compareTo(mList.get(i)) > 0) {
				best = mList.get(i);
			}
		}
		return best;
	}

	private void onTimeChanged(int source) {
		TimeClient best_tc = getBestSource();
		
		Intent intent = new Intent(Main.TIME_UPDATE_ACTION);
		intent.putExtra("accuracy", best_tc.getAccuracy());
		intent.putExtra("source", best_tc.getSource());
		intent.putExtra("diff", best_tc.getDifference());
		sendBroadcast(intent);
	}

}
