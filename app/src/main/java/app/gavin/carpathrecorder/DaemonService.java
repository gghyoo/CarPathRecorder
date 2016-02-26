package app.gavin.carpathrecorder;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.droidwolf.fix.FileObserver;

import java.util.List;
import java.util.concurrent.Semaphore;

public class DaemonService extends IntentService {

    FileObserver mProcessObserver;
    private final Semaphore mSemaphore = new Semaphore(0);
    private boolean stopFlag = false;

    public final String TAG = "DaemonService";
    public static final String SERVICE_ACTION = "app.gavin.carpathrecorder.action.DAEMON";
    public DaemonService() {
        super("DaemonService");
    }

    public static int getServicePid(Context context, String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE); //这个value取任意大于1的值，但返回的列表大小可能比这个值小。
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            String name = runningServiceInfo.service.getClassName();
            if (name.equals(serviceClassName)){
                return runningServiceInfo.pid;
            }
        }
        return -1;
    }
    public static void startServiceByAction(Context context, String action){
        Intent newIntent = new Intent(action);
        newIntent.setPackage(context.getPackageName());
        context.startService(newIntent);
    }
    public static int waitAndStartService(Context context, String serviceName, String action){
        final int maxRetry = 4, singleDelay = 1000;
        int pid = -1, retry = maxRetry;
        while (pid == -1) {
            pid = getServicePid(context, serviceName);
            try {
                if (pid == -1) {
                    if (retry++ >= maxRetry) {
                        startServiceByAction(context, action);
                        retry = 0;
                    }
                    Thread.sleep(singleDelay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return pid;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, TAG + " call onCreate @PID:" + android.os.Process.myPid());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, TAG + " call onStartCommand @PID:" + android.os.Process.myPid());
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            while(!stopFlag) {

                //get service pid
                int pid = waitAndStartService(getApplicationContext(),
                        LocationService.class.getName(), LocationService.SERVICE_ACTION);

                //建立FileObserver，监控进程是否存在
                mProcessObserver = new ProcessObserver(pid, new ProcessObserver.OnProcessExit() {
                    @Override
                    public void onProcessExit() {
                        mSemaphore.release();
                        mProcessObserver.stopWatching();
                    }
                });
                try {
                    mProcessObserver.startWatching();
                    Log.d(TAG, "Wait " + pid + " to be killed...");
                    mSemaphore.acquire();
                    Log.d(TAG, "======== Process " + pid + " has been killed! ========");
                    startServiceByAction(getApplicationContext(), LocationService.SERVICE_ACTION);
                    mProcessObserver = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
