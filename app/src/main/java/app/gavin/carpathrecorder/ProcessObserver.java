package app.gavin.carpathrecorder;

import com.droidwolf.fix.FileObserver;

/**
 * Created by Gavin on 2016/2/23.
 */
public class ProcessObserver extends FileObserver {

    public interface OnProcessExit{
        void onProcessExit();
    }

    OnProcessExit mOnProcessExit;

    int mObserverPid = -1;

    public ProcessObserver(int pid, OnProcessExit onProcessExit){
        super("/proc/"+pid, FileObserver.CLOSE_NOWRITE);
        mObserverPid = pid;
        mOnProcessExit = onProcessExit;
    }

    @Override
    public void onEvent(int event, String s) {
        if ((event & FileObserver.CLOSE_NOWRITE) == FileObserver.CLOSE_NOWRITE) {
            mOnProcessExit.onProcessExit();
        }
    }
}
