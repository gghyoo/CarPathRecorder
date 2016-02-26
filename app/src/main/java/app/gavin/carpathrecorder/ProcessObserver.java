package app.gavin.carpathrecorder;


import android.util.Log;

import com.droidwolf.fix.FileObserver;

import java.io.File;

/**
 * Created by Gavin on 2016/2/23.
 */
public class ProcessObserver extends FileObserver {

    public interface OnProcessExit{
        void onProcessExit();
    }

    OnProcessExit mOnProcessExit;

    int mObserverPid = -1;
    File mObserverFile = null;

    public ProcessObserver(int pid, OnProcessExit onProcessExit){
        super("/proc/"+pid, FileObserver.CLOSE_NOWRITE);
        mObserverPid = pid;
        mOnProcessExit = onProcessExit;
        mObserverFile = new File("/proc/"+pid);
    }

    public boolean IsProcessFileExist(){
        return mObserverFile.exists();
    }

    @Override
    public void onEvent(int event, String s) {
        if (((event & FileObserver.CLOSE_NOWRITE) == FileObserver.CLOSE_NOWRITE) && (!IsProcessFileExist())) {
            Log.d(ProcessObserver.class.getSimpleName(), ">>>>>> Pid = " + mObserverPid + " Event = " + event + " Path = " + s);
            mOnProcessExit.onProcessExit();
        }
    }
}
