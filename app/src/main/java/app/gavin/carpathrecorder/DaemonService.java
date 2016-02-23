package app.gavin.carpathrecorder;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import com.droidwolf.fix.FileObserver;


public class DaemonService extends IntentService {
    int mProtectedPid = -1;
    FileObserver mProcessObserver;

    private static final String ACTION_DAEMON = "app.gavin.carpathrecorder.action.DAEMON";
    public DaemonService() {
        super("DaemonService");
    }

    public static void startDaemonService(Context context) {
        Intent intent = new Intent(context, DaemonService.class);
        intent.setAction(ACTION_DAEMON);
        int pid = android.os.Process.myPid();
        intent.putExtra("PID", pid);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mProtectedPid = intent.getIntExtra("PID", -1);
            if(mProtectedPid != -1){
                //建立FileObserver，监控mProtectedPid进程是否存在
                mProcessObserver = new ProcessObserver(mProtectedPid, new ProcessObserver.OnProcessExit() {
                    @Override
                    public void onProcessExit() {

                    }
                });
            }
        }
    }
}
