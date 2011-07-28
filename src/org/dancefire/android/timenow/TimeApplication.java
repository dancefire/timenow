package org.dancefire.android.timenow;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;

public class TimeApplication extends Application {
    private static Context m_context;
    private static ProgressDialog m_pending_dialog = null;
    
    public void onCreate(){
    	m_context = getApplicationContext();
    	super.onCreate();
    }

    public static Context getAppContext() {
    	return m_context;
    }
    
    public static void showPendingDialog(Context context, String title, String message) {
    	m_pending_dialog = ProgressDialog.show(context, title, message);
    }
    
    public static void dismissPendingDialog() {
    	if (m_pending_dialog != null) {
    		m_pending_dialog.dismiss();
    	}
    }
}
