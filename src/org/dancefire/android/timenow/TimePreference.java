package org.dancefire.android.timenow;

import org.dancefire.android.timenow.service.TimeService;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class TimePreference extends PreferenceActivity {
	public static final String NTP_ENABLE = "pref_ntp_enable";
	public static final String NTP_SERVER = "pref_ntp_server";
	public static final String NTP_WIFI_ONLY = "pref_ntp_wifionly";
	public static final String GPS_ENABLE = "pref_gps_enable";
	public static final String TOAST_ENABLE = "pref_generic_toast";
	public static final String AUTO_SYNC = "pref_generic_autosync";
	public static final String ROOTLESS = "pref_generic_rootless";
	public static final String AUTO_TIMEZONE = "pref_generic_autotimezone";
	public static final String SYNC_ON_BOOT = "pref_generic_synconboot";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
		// sync summary
		Preference pref_ntp_server = findPreference(NTP_SERVER);
		pref_ntp_server.setSummary(getPreferences(MODE_PRIVATE).getString(
				NTP_SERVER, this.getString(R.string.pref_ntp_server_default)));
		pref_ntp_server
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						preference.setSummary(newValue.toString());
						return false;
					}
				});
	}

	@Override
	protected void onPause() {
		sendBroadcast(new Intent(TimeService.TIME_STATUS_UPDATE_ACTION));
		super.onPause();
	}
}
