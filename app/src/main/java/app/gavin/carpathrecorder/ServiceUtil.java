package app.gavin.carpathrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

/**
 * Created by Gavin on 2016/2/29.
 */
public class ServiceUtil {

    public static boolean isPackageInstalled(Context context, String packageName) {
        boolean hasInstalled = false;
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(PackageManager.PERMISSION_GRANTED);
        for (PackageInfo p : list) {
            if (packageName != null && packageName.equals(p.packageName)) {
                hasInstalled = true;
                break;
            }
        }
        return hasInstalled;
    }

    public static boolean isServiceRunning(Context context, String serviceClassName) {
        return -1 != getServicePid(context, serviceClassName);
    }

    public static int getServicePid(Context context, String serviceClassName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE); //这个value取任意大于1的值，但返回的列表大小可能比这个值小。
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            String name = runningServiceInfo.service.getClassName();
            if (name.equals(serviceClassName)) {
                return runningServiceInfo.pid;
            }
        }
        return -1;
    }

    public static void startServiceByAction(Context context, String action) {
        Intent newIntent = new Intent(action);
        context.startService(newIntent);
    }

    public static void invokeTimerService(Context context, String action){
        Log.d("LocationService", "LocationService wac called..");
        AlarmManager am = (AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        Intent intent = new Intent(context, BootReceiver.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long now = System.currentTimeMillis();
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, now, 30000, pi);
    }
    public static void cancelAlarmManager(Context context, String action) {
        Log.d("LocationService", "cancel AlarmManager to start ");
        Intent intent = new Intent(context,BootReceiver.class);
        intent.setAction(action);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm=(AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        alarm.cancel(pendingIntent);
    }
}
