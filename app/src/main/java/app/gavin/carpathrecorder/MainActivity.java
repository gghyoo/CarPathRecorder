package app.gavin.carpathrecorder;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.MyLocationStyle;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener, LocationSource,AMapLocationListener {

    final String TAG = "CarPathRecorder";
    static final String APK_CHANEL = "debug";
    PopupMenu mPopupMenu;

    private MapView mapView;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    LinearLayout mDownloadInfoBar;

    private String mNewApkUrl = "";

    public void ShowSnakeBar(String text, int time, String actionText, View.OnClickListener listener){
        final Snackbar snackbar = Snackbar.make(this.mapView, text, time);
        if(actionText != null)
            snackbar.setAction(actionText, listener);
        snackbar.show();
    }

    public void ShowSnakeBar(String text, int time){
        final Snackbar snackbar = Snackbar.make(this.mapView, text, time);
        snackbar.show();
    }

    private void DownloadApk(){
        File dir = getApplicationContext().getExternalFilesDir("update");
        if(dir == null){
            ShowSnakeBar("无法读写文件，可能没有安装SD卡!", Snackbar.LENGTH_LONG);
            return;
        }else if (!dir.exists() && !dir.mkdir()) {
            ShowSnakeBar("创建下载临时文件失败，请检查外部存储设备是否正常", Snackbar.LENGTH_LONG);
            return;
        }
        File apkFile = new File(dir.getPath() + "/" + getApplicationContext().getPackageName() + ".apk");
        mDownloadInfoBar.setVisibility(View.VISIBLE);
        ((ProgressBar)findViewById(R.id.downloadProgressBar)).setProgress(0);
        HttpClient.asyncGet(getApplicationContext(), mNewApkUrl, new FileAsyncHttpResponseHandler(apkFile) {
            long mStartTime = 0;
            DecimalFormat mFormater = new DecimalFormat("0.00");

            @Override
            public void onStart() {
                super.onStart();
                mStartTime = System.currentTimeMillis();
            }

            @Override
            public void onSuccess(int i, Header[] headers, File file) {
                mDownloadInfoBar.setVisibility(View.GONE);
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(intent);
            }

            @Override
            public void onFailure(int i, Header[] headers, Throwable throwable, File file) {
                ShowSnakeBar("更新程序下载失败! StatusCode：" + i, Snackbar.LENGTH_LONG);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                int percent = (int) ((100 * bytesWritten) / totalSize);
                long curTime = System.currentTimeMillis();
                float speed = ((float) bytesWritten) / (curTime - mStartTime);
                ((ProgressBar) findViewById(R.id.downloadProgressBar)).setProgress(percent);
                String info = percent + "%  " + mFormater.format(speed) + "k/s";
                ((TextView) findViewById(R.id.downloadTextInfoView)).setText(info);
            }
        });
    }

    private void GetApkInfo(){
        HttpClient.asyncGet(getApplicationContext(), LocationService.WEB_ACTION_URL + "getApkInfo", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                if (statusCode == 200) {
                    try {
                        boolean result = response.getBoolean("Result");
                        if(!result){
                            ShowSnakeBar("服务器文件出错！", Snackbar.LENGTH_LONG);
                            return;
                        }
                        JSONArray ja = response.getJSONArray("Data");
                        int cnt = ja.length();
                        for(int i = 0 ; i < cnt ; i ++){
                            JSONObject jo = ja.getJSONObject(i);
                            if(jo.getString("BuildType").equals(APK_CHANEL)){
                                PackageInfo pi = getApplicationContext().getPackageManager()
                                        .getPackageInfo(getApplicationContext().getPackageName(), 0);
                                String iv = pi.versionName;
                                if(iv.charAt(0) == 'v')
                                    iv = iv.substring(1);
                                String nv =jo.getString("VersionName");
                                if(nv.charAt(0) == 'v')
                                    iv = iv.substring(1);
                                if(nv.compareTo(iv) > 0){//比较新
                                    mNewApkUrl = LocationService.WEB_ACTION_URL + "getApk/package/" + jo.getString("Package") + "/channel/" + APK_CHANEL;
                                    ShowSnakeBar("检测到最新版本:" + nv, Snackbar.LENGTH_LONG, "开始下载", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            DownloadApk();
                                        }
                                    });
                                }
                                else
                                    ShowSnakeBar("当前为最新版本", Snackbar.LENGTH_LONG);
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        ShowSnakeBar("服务器错误！", Snackbar.LENGTH_LONG);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                ShowSnakeBar("检查更新失败", Snackbar.LENGTH_LONG);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.fab).setOnClickListener(this);

        mPopupMenu = new PopupMenu(this, findViewById(R.id.fab));
        mPopupMenu.getMenuInflater().inflate(R.menu.menu_main, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(this);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 必须要写
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        startLocationService();

        mDownloadInfoBar = (LinearLayout) findViewById(R.id.downloadInfoBar);
        mDownloadInfoBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_start_service) {
            if(!startLocationService())
                Log.d(TAG, getString(R.string.service_is_running));
           // LocationService.invokeTimerService(getApplicationContext());
            return true;
        }
        else if(id == R.id.action_close_service){
            if(!stopLocationService())
                Log.d(TAG, getString(R.string.service_is_not_running));
            LocationService.cancelAlarmManager(getApplicationContext());
            return true;
        }
        else if(id == R.id.action_update_apk){
            GetApkInfo();
            return true;
        }
        else if(id == R.id.action_test){

        }
        return super.onOptionsItemSelected(item);
    }


    boolean startLocationService(){
        if(LocationService.isServiceRunning(getApplicationContext(), LocationService.class.getName()))
            return false;
        Intent intent = new Intent("app.gavin.carpathrecorder.action.LOCATION");
        intent.setPackage(getPackageName());
        startService(intent);
        return true;
    }

    boolean stopLocationService(){
        if(!LocationService.isServiceRunning(getApplicationContext(), LocationService.class.getName()))
            return false;
        Intent intent = new Intent("app.gavin.carpathrecorder.action.LOCATION");
        intent.setPackage(getPackageName());
        stopService(intent);
        return true;
    }

    private void setUpMap() {
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
                .fromResource(R.drawable.location_marker));// 设置小蓝点的图标
        myLocationStyle.radiusFillColor(Color.argb(40, 0, 0, 200));
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setTrafficEnabled(true);
        aMap.setLocationSource(this);// 设置定位监听

        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(16));
    }

    private void updateInfos(AMapLocation location){
        LinearLayout gpsBar =(LinearLayout)findViewById(R.id.gpsInfoBar);
        gpsBar.setVisibility(View.VISIBLE);
        if(location.getLocationType() != AMapLocation.LOCATION_TYPE_SAME_REQ)
        {
            DecimalFormat df = new DecimalFormat("0.0");
            ((TextView)findViewById(R.id.locationText)).setText(getString(R.string.two_values,df.format(location.getLatitude()), df.format(location.getLongitude())));
            ((TextView)findViewById(R.id.accuracyText)).setText(getString(R.string.meter,((int)location.getAccuracy())));
            ((TextView)findViewById(R.id.altitudeText)).setText(getString(R.string.meter,(int)(location.getAltitude())));
            ((TextView)findViewById(R.id.directionText)).setText(getString(R.string.degree,((int)(location.getBearing()))));
            ((TextView)findViewById(R.id.speedText)).setText(getString(R.string.kmph, df.format(location.getSpeed() * 3.6)));
            ((TextView)findViewById(R.id.satelliteText)).setText(String.valueOf(location.getSatellites()));
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab)
            mPopupMenu.show();
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            AMapLocationClientOption option = new AMapLocationClientOption();
            mLocationClient.setLocationListener(this);
            option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationClient.setLocationOption(option);
            mLocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null
                    && aMapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                updateInfos(aMapLocation);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode()+ ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }
}
