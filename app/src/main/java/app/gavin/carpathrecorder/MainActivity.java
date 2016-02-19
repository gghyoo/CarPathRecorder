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
import android.widget.PopupMenu;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener, LocationSource,AMapLocationListener {

    final String TAG = "CarPathRecorder";
    static final String APK_URL = "http://gghyoo.github.io/apks/";
    static final String APK_CHANEL = "debug";
    static final String INFO_FILE_NAME = "info.txt";
    PopupMenu mPopupMenu;

    private MapView mapView;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private CameraUpdate mDefaultCameraUpdate = CameraUpdateFactory.zoomTo(18);

    private String mNewApkUrl = "";

    public void ShowSnakeBar(String text, int time, String actionText){
        final Snackbar snackbar = Snackbar.make(this.mapView, text, time);
        if(actionText != null)
            snackbar.setAction(actionText, this);
        snackbar.show();
    }

    public void copyFile(File oldFile, String newPath) {
        try {
            int byteRead = 0;
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldFile.getPath()); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteRead = inStream.read(buffer)) != -1)
                    fs.write(buffer, 0, byteRead);
                inStream.close();
            }
        }
        catch (Exception e) {
            Log.e(TAG, "复制单个文件操作出错");
        }
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
            LocationService.cancleAlarmManager(getApplicationContext());
            return true;
        }
        else if(id == R.id.action_update_apk){
            HttpClient.asyncGet(APK_URL + INFO_FILE_NAME, null, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    if (statusCode == 200) {
                        String response = new String(responseBody);
                        Log.d(TAG, response);
                        String[] lines = response.split("\n");
                        for(String line : lines){
                            if(line.contains(APK_CHANEL)){
                                String [] s = line.split("@");
                                String nv = s[1].substring(0, s[1].indexOf('_'));
                                if(nv.charAt(0) == 'v')
                                    nv = nv.substring(1);
                                try {
                                    PackageInfo pi = getApplicationContext().getPackageManager()
                                            .getPackageInfo(getApplicationContext().getPackageName(), 0);
                                    String iv = pi.versionName;
                                    if(iv.charAt(0) == 'v')
                                        iv = iv.substring(1);
                                    if(nv.compareTo(iv) > 0){//比较新
                                        mNewApkUrl = APK_URL + s[0] + ".apk";
                                        ShowSnakeBar("检测到最新版本:" + nv, Snackbar.LENGTH_LONG, "开始下载");
                                    }
                                    else
                                        ShowSnakeBar("当前为最新版本", Snackbar.LENGTH_LONG, null);
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                    ShowSnakeBar("检查更新失败", Snackbar.LENGTH_LONG, null);
                }
            });
            return true;
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
                .fromResource(R.drawable.car));// 设置小蓝点的图标
        myLocationStyle.radiusFillColor(Color.argb(40, 0, 0, 200));
        // myLocationStyle.anchor(int,int)//设置小蓝点的锚点
        //myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setTrafficEnabled(true);
        aMap.setLocationSource(this);// 设置定位监听

        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);

        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(22.52, 113.93), 15));
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab)
            mPopupMenu.show();
        else
        {
            String name = v.getClass().getName();
            String path = getApplicationContext().getExternalFilesDir("update").getPath();
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File apkFile = new File(path + "/" + getApplicationContext().getPackageName() + ".apk");
            HttpClient.asyncGet(mNewApkUrl, null, new FileAsyncHttpResponseHandler(apkFile) {
                @Override
                public void onSuccess(int i, Header[] headers, File file) {
                    String command  = "chmod 777 " + file.getPath();
                    Runtime runtime = Runtime.getRuntime();
                    try {
                        runtime.exec(command);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    startActivity(intent);
                }
                @Override
                public void onFailure(int i, Header[] headers, Throwable throwable, File file) {
                    Log.e(TAG, "Get Update apk failed!");
                }
                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                }
            });
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
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
                aMap.moveCamera(mDefaultCameraUpdate);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode()+ ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }
}
