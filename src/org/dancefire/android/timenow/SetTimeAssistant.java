package org.dancefire.android.timenow;

import java.text.DateFormat;

import org.dancefire.android.timenow.timeclient.TimeResult;
import org.dancefire.android.timenow.timeclient.Util;
import org.dancefire.android.timenow.timeclient.Util.DateFormatStyle;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public abstract class SetTimeAssistant {
	private Context m_context;
	private Handler m_handler_toast = null;
	private boolean m_enable_toast = false;
	private Toast m_toast = null;

	/* Constants */
	private static final int TOAST_ACTION = 1;
	private static final int TOAST_LENGTH_SHORT = 1000;

	public SetTimeAssistant(Context context) {
		m_context = context;

		// Toast handler
		m_handler_toast = new Handler() {
			public void handleMessage(android.os.Message msg) {
				if (msg.what == TOAST_ACTION) {
					// repeat
					m_handler_toast.sendEmptyMessageDelayed(TOAST_ACTION,
							TOAST_LENGTH_SHORT - 100);
					showToast();
				}
			}
		};

		createToast();

	}

	/* General function */
	public void start() {
		enableToast(true);
	}

	public void stop() {
		enableToast(false);
	}

	/* Toast */
	public void enableToast(boolean enable) {
		if (m_enable_toast != enable) {
			if (enable) {
				m_handler_toast.removeMessages(TOAST_ACTION);
				m_handler_toast.sendEmptyMessage(TOAST_ACTION);
			} else {
				m_handler_toast.removeMessages(TOAST_ACTION);
				m_toast.cancel();
			}
			Log.v(Main.TAG, "SetTimeAssistant.enableToast(" + m_enable_toast + " => " + enable + ")");
			m_enable_toast = enable;
		}
	}

	abstract protected TimeResult getTimeResult();

	private void createToast() {
		m_toast = new Toast(m_context);
		m_toast.setDuration(Toast.LENGTH_SHORT);
		m_toast.setGravity(Gravity.BOTTOM | Gravity.RIGHT, 10, 0);
		m_toast.setMargin(0.02f, 0.02f);
		LayoutInflater inflater = (LayoutInflater) (m_context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
		View toast_view = inflater.inflate(R.layout.toast, null);
		m_toast.setView(toast_view);
	}

	private void showToast() {
		TimeResult time_result = getTimeResult();
		if (m_toast != null
				&& time_result != null
				&& TimePreference.get().getBoolean(TimePreference.TOAST_ENABLE,
						true)) {
			TextView tvMessage = (TextView) m_toast.getView().findViewById(
					R.id.toast_message);

			long diff = time_result.getLocalTimeError();
			long t = time_result.getCurrentSourceTime();
			String date = DateFormat.getDateInstance(DateFormat.LONG).format(t);
			String time = Util.formatDateTime(t, DateFormatStyle.TIME_ONLY);
			String offset = Util.getTimeSpanNumericString(diff);

			// Only show toast when the time error is larger than 30 seconds
			long thirty_seconds = 30 * Util.TIME_ONE_SECOND;
			if (Math.abs(diff) > thirty_seconds) {
				String fmt = m_context.getString(R.string.toast_message);
				tvMessage.setText(String.format(fmt, date, time, offset));
				m_toast.setDuration(Toast.LENGTH_SHORT);
			} else {
				// Stop toast handler
				enableToast(false);
				// Show new local time error
				String fmt = m_context.getString(R.string.toast_message_ok);
				tvMessage.setText(String.format(fmt, offset));
				m_toast.setDuration(Toast.LENGTH_LONG);
			}
			m_toast.show();
			Log.v(Main.TAG, "SetTimeAssistant.showToast()");
		}
	}
}
