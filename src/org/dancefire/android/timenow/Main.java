package org.dancefire.android.timenow;

import java.util.ArrayList;
import java.util.Collections;

import org.dancefire.android.timenow.service.TimeService;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;
import org.dancefire.android.timenow.timeclient.Util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

public class Main extends Activity {
	public static final String TIME_UPDATE_ACTION = "org.dancefire.android.action.TIME_UPDATE";
	public static final int UPDATE_UI_ACTION = 0;
	public static final int SERVICE_START_ACTION = 1;
	public static final String TAG = "TimeNow";

	private Handler handlerUIUpdate;
	private Handler handlerService;
	private BroadcastReceiver receiver;
	private ArrayList<TimeResult> time_list;
	private TextView textPhoneTime;
	private ListView listSourceTime;
	private TimeResultAdapter time_result_adapter;

	private static final int UPDATE_DELAY = 200;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		time_list = new ArrayList<TimeResult>();
		setHandler();
		setReceiver();

		textPhoneTime = (TextView) findViewById(R.id.phone_time);
		listSourceTime = (ListView) findViewById(R.id.list_time);
		time_result_adapter = new TimeResultAdapter(this, this.time_list);
		listSourceTime.setAdapter(time_result_adapter);
	}

	@Override
	protected void onResume() {
		// Start Service
		startService(new Intent(this, TimeService.class));

		// Register Receiver
		registerReceiver(receiver, new IntentFilter(Main.TIME_UPDATE_ACTION));

		// Begin message loop
		handlerUIUpdate.sendEmptyMessage(UPDATE_UI_ACTION);
		handlerService.sendEmptyMessage(SERVICE_START_ACTION);
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// Stop Service
		stopService(new Intent(this, TimeService.class));
		Log.d(Main.TAG, "Stoping Time service");

		// Unregister Receiver
		unregisterReceiver(receiver);
		Log.d(Main.TAG, "Unregistering receiver");

		// stop message loop
		handlerUIUpdate.removeMessages(UPDATE_UI_ACTION);
		handlerService.removeMessages(SERVICE_START_ACTION);
		super.onPause();
	}

	private void setHandler() {
		// Setup UI update handler
		handlerUIUpdate = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == UPDATE_UI_ACTION) {
					updateUI();
					handlerUIUpdate.sendEmptyMessageDelayed(UPDATE_UI_ACTION,
							UPDATE_DELAY);
				}
			};
		};
		// Service handler
		handlerService = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == SERVICE_START_ACTION) {
					// Start Service
					handlerService.sendEmptyMessageDelayed(
							SERVICE_START_ACTION, TimeClient.INTERVAL_LONG);
					Main.this.startService(new Intent(Main.this,
							TimeService.class));
				}
			}
		};
	}

	private void setReceiver() {
		// Setup broadcast receiver
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				TimeResult result = TimeResult.fromBundle(intent.getExtras());
				long diff = result.getLocalTimeError();
				Log.v(Main.TAG, "Received broadcast from [" + result.source
						+ "] = " + diff);
				updateResultList(result);
				updateUI();
			}
		};
	}

	private void updateResultList(TimeResult result) {
		boolean modified = false;
		int index = time_list.indexOf(result);
		if (index >= 0) {
			// found a match
			// replace the new one if the latest is better than previous result.
			TimeResult pre = time_list.get(index);
			if (pre.accuracy > result.accuracy) {
				time_list.set(index, result);
				modified = true;
			}
		} else {
			// add a new result
			time_list.add(result);
			modified = true;
		}
		if (modified) {
			// Sort the time list by accuracy
			Collections.sort(time_list);
			updateUI();
		}
	}

	private void updateUI() {
		textPhoneTime.setText(Util.formatDateTime(System.currentTimeMillis(),
				Util.DateFormatStyle.FULL));
		time_result_adapter.notifyDataSetChanged();
	}
}
