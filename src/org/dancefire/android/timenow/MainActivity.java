package org.dancefire.android.timenow;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static class TimeResultAdapter extends BaseAdapter {
		private LayoutInflater m_inflater;
		private Bitmap m_icon_gps;
		private Bitmap m_icon_ntp;
		private ArrayList<TimeResult> m_result_list;

		public TimeResultAdapter(Context context,
				ArrayList<TimeResult> result_list) {
			m_inflater = LayoutInflater.from(context);
			m_icon_gps = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.gps_32);
			m_icon_ntp = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ntp_32);
			m_result_list = result_list;
		}

		@Override
		public int getCount() {
			return m_result_list.size();
		}

		@Override
		public Object getItem(int position) {
			return m_result_list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = m_inflater.inflate(R.layout.time_result_item,
						null);

				holder = new ViewHolder();
				holder.icon = (ImageView) convertView
						.findViewById(R.id.time_result_item_icon);
				holder.title = (TextView) convertView
						.findViewById(R.id.time_result_item_title);
				holder.subtitle1 = (TextView) convertView
						.findViewById(R.id.time_result_item_subtitle1);
				holder.subtitle2 = (TextView) convertView
						.findViewById(R.id.time_result_item_subtitle2);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			TimeResult item = m_result_list.get(position);
			switch (item.source) {
			case TimeClient.TIME_NTP:
				holder.icon.setImageBitmap(m_icon_ntp);
				break;
			case TimeClient.TIME_GPS:
				holder.icon.setImageBitmap(m_icon_gps);
				break;
			default:
				holder.icon.setImageResource(R.drawable.icon);
				break;
			}
			holder.title.setText(TimeClient.fmtLong.format(item
					.getCurrentTime()));
			holder.subtitle1.setText(TimeResult.getTimeSpanString(item.getDifference()));
			holder.subtitle2.setText(TimeResult.getTimeSpanString(item.phone_time - System.currentTimeMillis()));

			return convertView;
		}

		static class ViewHolder {
			ImageView icon;
			TextView title;
			TextView subtitle1;
			TextView subtitle2;
		}

	}

	public static final String TIME_UPDATE_ACTION = "org.dancefire.android.action.TIME_UPDATE";
	public static final int UPDATE_UI_ACTION = 0;
	public static final String TAG = "TimeNow";

	private Handler handler;
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
		handler.sendEmptyMessage(UPDATE_UI_ACTION);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// Stop Service
		stopService(new Intent(this, TimeService.class));
		Log.i(Main.TAG, "Stoping Time service");

		// Unregister Receiver
		unregisterReceiver(receiver);
		Log.i(Main.TAG, "Unregistering receiver");

		// stop message loop
		handler.removeMessages(UPDATE_UI_ACTION);
		super.onPause();
	}

	private void setHandler() {
		// Setup update handler
		handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == UPDATE_UI_ACTION) {
					updateTime();
					handler.sendEmptyMessageDelayed(UPDATE_UI_ACTION,
							UPDATE_DELAY);
				}
			};
		};
	}

	private void setReceiver() {
		// Setup broadcast receiver
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int source = intent.getIntExtra("source", TimeClient.TIME_NONE);
				long diff = intent.getLongExtra("diff", 0);
				Log.i(Main.TAG, "Received broadcast from [" + source + "] = "
						+ diff);
				switch (source) {
				case TimeClient.TIME_NTP:
					// diff_ntp = diff;
					TimeResult item_ntp = new TimeResult(TimeClient.TIME_NTP);
					item_ntp.source_time = item_ntp.phone_time + diff;
					item_ntp.accuracy = 10;
					time_list.add(item_ntp);
					break;
				case TimeClient.TIME_GPS:
					TimeResult item_gps = new TimeResult(TimeClient.TIME_GPS);
					item_gps.source_time = item_gps.phone_time + diff;
					item_gps.accuracy = 10;
					time_list.add(item_gps);
					break;
				}
				updateTime();
			}
		};
	}

	private void updateTime() {
		textPhoneTime.setText(TimeClient.fmtLong.format(System
				.currentTimeMillis()));
		time_result_adapter.notifyDataSetChanged();
	}
}
