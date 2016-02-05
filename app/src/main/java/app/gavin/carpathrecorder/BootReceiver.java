package app.gavin.carpathrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("BootReceiver", "From Gavin's App: system boot completed");

            // context, AutoRun.class
            Intent newIntent = new Intent(context, MainActivity.class);

			/* MyActivity action defined in AndroidManifest.xml */
            newIntent.setAction("android.intent.action.MAIN");

			/* MyActivity category defined in AndroidManifest.xml */
            newIntent.addCategory("android.intent.category.LAUNCHER");

			/*
			 * If activity is not launched in Activity environment, this flag is
			 * mandatory to set
			 */
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			/* if you want to start a service, follow below method */
            context.startActivity(newIntent);
        }
        else if(action.equals("android.intent.action.USER_PRESENT"))
        {
            Log.d("User_present", "android.intent.action.USER_PRESENT: User Present");

            Intent newIntent = new Intent(context, MainActivity.class);

			/* MyActivity action defined in AndroidManifest.xml */
            newIntent.setAction("android.intent.action.MAIN");

			/* MyActivity category defined in AndroidManifest.xml */
            newIntent.addCategory("android.intent.category.LAUNCHER");

			/*
			 * If activity is not launched in Activity environment, this flag is
			 * mandatory to set
			 */
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			/* if you want to start a service, follow below method */
            context.startActivity(newIntent);
        }
    }
}