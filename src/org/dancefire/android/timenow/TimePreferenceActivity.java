package org.dancefire.android.timenow;
import org.dancefire.android.timenow.service.TimeService;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TimePreferenceActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
	
	@Override
	protected void onPause() {
		sendBroadcast(new Intent(TimeService.TIME_STATUS_UPDATE_ACTION));
		super.onPause();
	}
}
