package org.dancefire.android.timenow;

import android.app.Application;
import android.content.Context;

public class TimeApplication extends Application {
    private static Context context;

    public void onCreate(){
    	context = getApplicationContext();
    	super.onCreate();
    }

    public static Context getAppContext() {
    	return context;
    }
}
