package app.gavin.carpathrecorder;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.util.Log;

import com.droidwolf.nativesubprocess.Subprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Gavin on 2016/2/23.
 */
public class ObserverSubProcess extends Subprocess{
    static final String mServiceName = "app.gavin.carpathrecorder:remote";

    boolean mStopObserveFlag = false;
    int mCheckInterval = 60 * 1000;

    public static boolean isServiceAliveByPid(int pid){
        try {
            Process p = Runtime.getRuntime().exec("ps");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                String [] words = line.split("\\s+");
                if (words.length > 0 && words[1].matches("\\d+") && Integer.parseInt(words[1]) == pid)
                    return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isServiceAliveByName(String name){
        try {
            Process p = Runtime.getRuntime().exec("ps");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            int pid = android.os.Process.myPid();
            while ((line = in.readLine()) != null) {
                if(line.contains(name)) {
                    String[] words = line.split("\\s+");
                    if ((words.length > 0) && words[1].matches("\\d+") && (Integer.parseInt(words[1]) != pid))
                        return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean startObserver(Context context){
        SharedPreferences settings = context.getSharedPreferences("setting", 0);
        if(settings.getBoolean("EnableObserve", true)){
            Log.d(ObserverSubProcess.class.getSimpleName(), "Create SubProcess...");
            Subprocess.create(context, ObserverSubProcess.class);
        }
        return true;
    }

    public void restartService(){
        try {
			Runtime.getRuntime().exec("am startservice --user 0 -n app.gavin.carpathrecorder/app.gavin.carpathrecorder.LocationService");
		} catch (IOException e) {
            Log.e(ObserverSubProcess.class.getSimpleName(), "Start Service Failed with message: " + e.getMessage());
        }
    }

    @Override
    public void runOnSubprocess() {
        Log.d(ObserverSubProcess.class.getSimpleName(), "runOnSubprocess @PID:" + android.os.Process.myPid() + " Parent PID:" + getParentPid());
        while(!mStopObserveFlag) {
            try {
                Log.d(ObserverSubProcess.class.getSimpleName(), "Checking Observed Service Status");

                //Check Service Status
                Log.d(getClass().getSimpleName(), "Check Service Status " + " @PID:" + android.os.Process.myPid() + " with ParentPID:" + getParentPid());
                while (!mStopObserveFlag && isServiceAliveByName(mServiceName)) {
                    Thread.sleep(mCheckInterval);
                    Log.d(getClass().getSimpleName(), "Check Service Status " + " @PID:" + android.os.Process.myPid() + " with ParentPID:" + getParentPid());
                }

                if(mStopObserveFlag)
                    break;

                Log.d(getClass().getSimpleName(), "Service is Not Alive");
                //Service Not alive
                Log.d(getClass().getSimpleName(), "Restart Service");
                //Restart Service
                restartService();

                break;

            }catch (InterruptedException e) {
                Log.d(getClass().getSimpleName(), "runOnSubprocess Exception:" + e.getMessage());
                e.printStackTrace();
            }
        }

        Log.d(getClass().getSimpleName(), "Observe SubProcess Exits @PID:" + android.os.Process.myPid());
        System.exit(0);
    }
}
