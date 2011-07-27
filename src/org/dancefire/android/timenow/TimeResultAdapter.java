/**
 * 
 */
package org.dancefire.android.timenow;

import java.util.ArrayList;

import org.dancefire.android.timenow.timeclient.NtpTimeClient;
import org.dancefire.android.timenow.timeclient.TimeClient;
import org.dancefire.android.timenow.timeclient.TimeResult;
import org.dancefire.android.timenow.timeclient.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class TimeResultAdapter extends BaseAdapter {
	private LayoutInflater m_inflater;
	private Bitmap m_icon_gps;
	private Bitmap m_icon_ntp;
	private ArrayList<TimeResult> m_result_list;

	public TimeResultAdapter(Context context, ArrayList<TimeResult> result_list) {
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
		TimeResultAdapter.ViewHolder holder;

		if (convertView == null) {
			convertView = m_inflater.inflate(R.layout.time_result_item, null);

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
			holder = (TimeResultAdapter.ViewHolder) convertView.getTag();
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
		holder.title.setText(Util.formatDateTime(item.getCurrentSourceTime(),
				Util.DateFormatStyle.FULL));
		StringBuilder sb1 = new StringBuilder();
		sb1.append(Util.getTimeSpanString(item.getLocalTimeError()));
		sb1.append(" ");
		if (item.source == TimeClient.TIME_NTP) {
			sb1.append(item.extra.getString(NtpTimeClient.IP));
		} else {
			sb1.append(item.id);
		}
		holder.subtitle1.setText(sb1.toString());

		StringBuilder sb = new StringBuilder();
		switch (item.source) {
		case TimeClient.TIME_GPS:
			sb.append(item.accuracy);
			break;
		case TimeClient.TIME_NTP:
			sb.append(item.accuracy);
			break;
		default:
			sb.append(Util.getTimeSpanString(item.local_time
					- System.currentTimeMillis()));
		}
		holder.subtitle2.setText(sb.toString());

		return convertView;
	}

	static class ViewHolder {
		ImageView icon;
		TextView title;
		TextView subtitle1;
		TextView subtitle2;
	}

}