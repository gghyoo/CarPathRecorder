package app.gavin.carpathrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    final String TAG = "BootReceiver";
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "Received Action:" + action);

       // Intent newIntent = new Intent(context, MainActivity.class);

       // newIntent.setAction("android.intent.action.MAIN");

      //  newIntent.addCategory("android.intent.category.LAUNCHER");

      //  newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

       // context.startActivity(newIntent);

        if(!LocationService.isServiceRunning(context, LocationService.class.getName())) {
            Intent serviceIntent = new Intent("app.gavin.carpathrecorder.action.LOCATION");
            serviceIntent.setPackage("app.gavin.carpathrecorder");
            context.startService(serviceIntent);
            Log.d(TAG, context.getString(R.string.service_started));
        }
        else
            Log.d(TAG, context.getString(R.string.service_is_running));
    }
}