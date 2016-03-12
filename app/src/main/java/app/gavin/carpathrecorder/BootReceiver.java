package app.gavin.carpathrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    final String TAG = "BootReceiver";
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "Received Action:" + action);
        if(!LocationService.isServiceRunning(context, LocationService.class.getName())) {
            SharedPreferences settings = context.getSharedPreferences("setting", 0);
            if(settings.getBoolean("EnableObserve", true)){
                Intent serviceIntent = new Intent("app.gavin.carpathrecorder.action.LOCATION");
                serviceIntent.setPackage("app.gavin.carpathrecorder");
                context.startService(serviceIntent);
                Log.d(TAG, context.getString(R.string.service_started));
            }
        }
        else
            Log.d(TAG, context.getString(R.string.service_is_running));
    }
}