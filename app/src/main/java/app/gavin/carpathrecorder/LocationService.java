package app.gavin.carpathrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class LocationService extends IntentService implements AMapLocationListener{

    private static final String TAG = "LocationService";
    private static final String SITE_URL = "http://s.imscv.com/cpr/";
    private static final String ACTION_URL = SITE_URL + "Recorder/Index/";
    private static final String SERVICE_ACTION = "app.gavin.carpathrecorder.action.LOCATION";

    private static final int MAX_LOCATION_DELAY = 10;
    private static final int MIN_LOCATION_DELAY = 1;

    private SyncHttpClient mHttpClient = new SyncHttpClient();
    private boolean mHttpBusyFlag = false;
    private Cursor mUnUploadedRecordCursor = null;

    private boolean mStopFlag = false;

    Notification mNotification;
    RemoteViews mNotificationRemoteView;

    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;
    ContentValues mLocationInfo = new ContentValues();

    private LocationDatabase mLocalDatabase = null;

    private long mLastInsertID = -1, mLastUpdateID = -1;
    private boolean mLocationStatus = false;
    private int mLocationDelay = 1;
    private int mLocationDelayCnt = 0;

    public LocationService() {
        super("LocationService");
    }
    public void setupAmpSdk(){
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        mLocationOption = new AMapLocationClientOption();
        // 设置定位模式为GPS
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setInterval(1000);
        mLocationOption.setNeedAddress(false);
        mLocationClient.setLocationOption(mLocationOption);
        // 设置定位监听
        mLocationClient.setLocationListener(this);
        mLocationClient.startLocation();
    }
    public void UploadLocationRecord(Cursor c){
        String json = LocationDatabase.cursorToJson(c);
        // 创建请求参数的封装的对象
        RequestParams params = new RequestParams();
        params.put("JsonData", json);// 设置请求的参数名和参数
        mHttpBusyFlag = true;
        mUnUploadedRecordCursor = c;
        String url = ACTION_URL + "addRecords";
        mHttpClient.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  byte[] responseBody) {
                if (statusCode == 200) {
                    String response = new String(responseBody);
                    Log.d(TAG, response);

                    //更新数据库内容
                    if (response.equals("Ok") && mUnUploadedRecordCursor != null) {
                        mLastUpdateID = mLocalDatabase.updateUploadStatus(mUnUploadedRecordCursor);
                        mUnUploadedRecordCursor = null;
                        updateNotification();
                    }

                    mHttpBusyFlag = false;
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                mHttpBusyFlag = false;
                error.printStackTrace();// 把错误信息打印出轨迹来
            }
        });
    }
    public void updateNotification(){
        DecimalFormat df = new DecimalFormat("0.00");
        mNotificationRemoteView.setTextViewText(R.id.total_local_record_count, mLastInsertID + getString(R.string.unit));
        mNotificationRemoteView.setTextViewText(R.id.total_uploaded_record_count, mLastUpdateID + getString(R.string.unit));
        mNotificationRemoteView.setTextViewText(R.id.location_status,
                mLocationStatus ? getString(R.string.status_ok) : getString(R.string.status_failed));
        if(mLocationStatus) {
            mNotificationRemoteView.setTextViewText(R.id.location, "(" + df.format(mLocationInfo.get("latitude"))
                    + " , " + df.format(mLocationInfo.get("longitude"))
                    + " , " + df.format(mLocationInfo.get("accuracy")) + " m)");
            mNotificationRemoteView.setTextViewText(R.id.altitude, df.format(mLocationInfo.get("altitude")) + " m");
            mNotificationRemoteView.setTextViewText(R.id.speed, df.format(mLocationInfo.get("speed")) + " m/s");
            mNotificationRemoteView.setTextViewText(R.id.direction, df.format(mLocationInfo.get("bearing")) + "");
            mNotificationRemoteView.setTextViewText(R.id.satellite, mLocationInfo.get("satellites") + "");
        }
        startForeground(7788, mNotification);
    }
    private void setupNotification() {
        //初始化通知Remote View
        mNotificationRemoteView = new RemoteViews(getPackageName(), R.layout.notification_view);
        //实例化通再来管理器
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContent(mNotificationRemoteView)
                .setTicker(getText(R.string.path_is_recording)) //通知首次出现在通知栏，带上升动画效果的
                .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
                        //  .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(true)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                        //.setDefaults(Notification.De)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合
                        //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.drawable.small_logo);//设置通知小ICON
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        mNotification = builder.build();
    }
    private int getLocationDelay(AMapLocation location){
        int interval;
        if(location.getProvider().equals("lbs"))
            interval = MAX_LOCATION_DELAY;
        else{
            float speed = location.getSpeed();
            interval = (int) (MAX_LOCATION_DELAY - speed * (MAX_LOCATION_DELAY - MIN_LOCATION_DELAY) / 10);
            if(interval < MIN_LOCATION_DELAY)
                interval = MIN_LOCATION_DELAY;
        }
        return interval;
    }

    public static boolean isServiceRunning(Context context, String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE); //这个value取任意大于1的值，但返回的列表大小可能比这个值小。
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            String name = runningServiceInfo.service.getClassName();
            if (name.equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }
    public static void invokeTimerService(Context context){
        Log.d("LocationService", "LocationService wac called..");
        PendingIntent alarmSender = null;
        Intent startIntent = new Intent(context, LocationService.class);
        startIntent.setAction(LocationService.SERVICE_ACTION);
        try {
            alarmSender = PendingIntent.getService(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } catch (Exception e) {
            Log.e("LocationService", "failed to start " + e.toString());
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60 * 1000, alarmSender);
    }
    public static void cancleAlarmManager(Context context){
        Log.d("LocationService", "cancleAlarmManager to start ");
        Intent intent = new Intent(context,LocationService.class);
        intent.setAction(LocationService.SERVICE_ACTION);
        PendingIntent pendingIntent=PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm=(AlarmManager)context.getSystemService(Activity.ALARM_SERVICE);
        alarm.cancel(pendingIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotification();
        updateNotification();
        setupAmpSdk();
        mLocalDatabase = new LocationDatabase();
        LocationService.invokeTimerService(getApplicationContext());
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != mLocationClient) {
            mLocationClient.onDestroy();
            mLocationClient = null;
            mLocationOption = null;
        }

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            while(!mStopFlag) {
                try {
                    Thread.sleep(mLocationDelay * 1000 * 10);
                    //如果上次的请求未结束，则等待结束后，再进行新的HTTP数据传输
                    if(mHttpBusyFlag)
                        continue;
                    //读取出所有的未记录内容
                    Cursor c = mLocalDatabase.queryNotUploadedRecord(64);
                    if(c == null)
                        continue;

                    if(c.getCount() <= 0)
                        continue;

                    //将内容上传到网络上
                    UploadLocationRecord(c);

                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间
                
                mLocationInfo.put("locationType", aMapLocation.getLocationType()); //获取当前定位结果来源，如网络定位结果，详见定位类型表
                mLocationInfo.put("provider", aMapLocation.getProvider());//提供方
                mLocationInfo.put("latitude", aMapLocation.getLatitude());//纬度
                mLocationInfo.put("longitude", aMapLocation.getLongitude());//经度
                mLocationInfo.put("altitude", aMapLocation.getAltitude());//海拔
                mLocationInfo.put("accuracy", aMapLocation.getAccuracy());//定位精度
                mLocationInfo.put("gpsTime", df.format(date));//定位时间
                mLocationInfo.put("satellites", aMapLocation.getSatellites());//卫星数
                mLocationInfo.put("speed", aMapLocation.getSpeed());//速度
                mLocationInfo.put("bearing", aMapLocation.getBearing());//航向

                //屏蔽掉重复定位数据
                if(aMapLocation.getLocationType() != AMapLocation.LOCATION_TYPE_SAME_REQ) {
                    //根据定位类型，更新速度
                    mLocationDelay = getLocationDelay(aMapLocation);
                    if (mLocationDelayCnt++ == 0)
                        mLastInsertID = mLocalDatabase.addLocationPoint(mLocationInfo);//写入数据库
                    if (mLocationDelayCnt >= mLocationDelay)
                        mLocationDelayCnt = 0;

                    Log.d(TAG, "mLocationDelay = " + mLocationDelay + ", mLocatonDelayCnt = " + mLocationDelayCnt);
                }

                mLocationStatus = true;
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError","location Error, ErrCode: "
                        + aMapLocation.getErrorCode() + ", errInfo: "
                        + aMapLocation.getErrorInfo());
                mLocationStatus = false;
            }
            updateNotification();
        }
    }
}
