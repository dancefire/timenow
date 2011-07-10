package org.dancefire.android.timenow;

import android.app.Application;
import android.content.Context;

public class TimeApplication extends Application {
    private static Context m_context;

    public void onCreate(){
    	m_context = getApplicationContext();
    	super.onCreate();
    }

    public static Context getAppContext() {
    	return m_context;
    }
}
