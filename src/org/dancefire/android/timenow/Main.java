package org.dancefire.android.timenow;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;

import org.dancefire.android.timenow.service.TimeService;
import org.dancefire.android.timenow.timeclient.TimeResult;
import org.dancefire.android.timenow.timeclient.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class Main extends Activity {
	public static final String TIME_UPDATE_ACTION = "org.dancefire.android.action.TIME_UPDATE";
	public static final int UPDATE_UI_ACTION = 0;
	public static final int PREFERENCE_UPDATE = 2;
	public static final String TAG = "TimeNow";

	private Handler m_handlerUIUpdate;
	private BroadcastReceiver m_receiver;
	private ArrayList<TimeResult> m_time_list;
	private TextView m_textPhoneTime;
	private ListView m_listSourceTime;
	private TimeResultAdapter m_time_result_adapter;
	private boolean m_show_toast = false;

	private static final int UPDATE_DELAY = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		m_time_list = new ArrayList<TimeResult>();
		setHandler();
		setReceiver();

		m_textPhoneTime = (TextView) findViewById(R.id.phone_time);
		m_listSourceTime = (ListView) findViewById(R.id.list_time);
		m_time_result_adapter = new TimeResultAdapter(this, this.m_time_list);
		m_listSourceTime.setAdapter(m_time_result_adapter);

		m_textPhoneTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (m_time_list != null && m_time_list.size() > 0) {
					m_show_toast = !m_show_toast;
					sendBroadcast(new Intent(TimeService.TIME_TOAST_ACTION)
							.putExtra(TimeService.SHOW_TOAST, m_show_toast));
				}
			}
		});

		// Start Service
		startService(new Intent(Main.this, TimeService.class));
	}

	@Override
	protected void onResume() {
		// Register Receiver
		registerReceiver(m_receiver, new IntentFilter(Main.TIME_UPDATE_ACTION));

		// Begin message loop
		m_handlerUIUpdate.sendEmptyMessage(UPDATE_UI_ACTION);

		// Tell service the UI is visible now
		sendBroadcast(new Intent(TimeService.TIME_UI_ACTION).putExtra(
				TimeService.IS_VISIBLE, true));

		updateUI();
		super.onResume();
		TimeApplication.dismissPendingDialog();
	}

	@Override
	protected void onPause() {
		// Unregister Receiver
		unregisterReceiver(m_receiver);
		Log.d(Main.TAG, "Unregistering receiver");

		// stop message loop
		m_handlerUIUpdate.removeMessages(UPDATE_UI_ACTION);

		// Tell service the UI is NOT visible now
		sendBroadcast(new Intent(TimeService.TIME_UI_ACTION).putExtra(
				TimeService.IS_VISIBLE, false));

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// Stop Service
		stopService(new Intent(this, TimeService.class));
		Log.d(Main.TAG, "Stoping Time service");

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.w(Main.TAG, item.getItemId() + ": " + item.toString());
		switch (item.getItemId()) {
		case R.id.menu_synchronize:
			sendBroadcast(new Intent(TimeService.TIME_TOAST_ACTION).putExtra(
					TimeService.SHOW_TOAST, true));
			startActivity(new Intent(
					android.provider.Settings.ACTION_DATE_SETTINGS));
			break;
		case R.id.menu_setting:
			Intent intent_pref = new Intent().setClass(this,
					TimePreference.class);
			this.startActivityForResult(intent_pref, PREFERENCE_UPDATE);
			break;
		case R.id.menu_about:
			new AlertDialog.Builder(this).setTitle(R.string.about_title)
					.setMessage(R.string.about_message)
					.setIcon(R.drawable.icon)
					.setNeutralButton(android.R.string.ok, null).show();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PREFERENCE_UPDATE) {
			// SharedPreferences pref =
			// PreferenceManager.getDefaultSharedPreferences(this);

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setHandler() {
		// Setup UI update handler
		m_handlerUIUpdate = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == UPDATE_UI_ACTION) {
					updateUI();
					m_handlerUIUpdate.sendEmptyMessageDelayed(UPDATE_UI_ACTION,
							UPDATE_DELAY);
				}
			};
		};
	}

	private void setReceiver() {
		// Setup broadcast receiver
		m_receiver = new BroadcastReceiver() {
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
		int index = m_time_list.indexOf(result);
		if (index >= 0) {
			// found a match
			// replace the new one if the latest is better than previous result.
			TimeResult pre = m_time_list.get(index);
			if (pre.accuracy > result.accuracy) {
				m_time_list.set(index, result);
				modified = true;
			}
		} else {
			// add a new result
			m_time_list.add(result);
			modified = true;
		}
		if (modified) {
			// Sort the time list by accuracy
			Collections.sort(m_time_list);
			updateUI();
		}
	}

	private void updateUI() {
		long t = System.currentTimeMillis();
		m_textPhoneTime.setText(DateFormat.getDateInstance(DateFormat.LONG)
				.format(t)
				+ " "
				+ Util.formatDateTime(System.currentTimeMillis(),
						Util.DateFormatStyle.TIME_ONLY));
		m_time_result_adapter.notifyDataSetChanged();
	}
}
