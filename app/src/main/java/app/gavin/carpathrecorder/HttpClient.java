package app.gavin.carpathrecorder;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

/**
 * Created by Gavin on 2016/2/18.
 */
public class HttpClient {
    private static AsyncHttpClient mAsyncClient = null;
    private static SyncHttpClient mSyncClient = null;

    private static AsyncHttpClient getAsyncClient(Context context){
        if(mAsyncClient == null){
            mAsyncClient = new AsyncHttpClient();
        //    mAsyncClient.addHeader("Accept-Encoding", "gzip");
            PersistentCookieStore myCookieStore = new PersistentCookieStore(context);
            mAsyncClient.setCookieStore(myCookieStore);
        }
        return mAsyncClient;
    }

    private static AsyncHttpClient getSyncClient(Context context){
        if(mSyncClient == null){
            mSyncClient = new SyncHttpClient();
        //    mSyncClient.addHeader("Accept-Encoding", "gzip");
            PersistentCookieStore myCookieStore = new PersistentCookieStore(context);
            mSyncClient.setCookieStore(myCookieStore);
        }
        return mSyncClient;
    }

    public static void asyncGet(Context c, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        getAsyncClient(c).get(url, params, responseHandler);
    }

    public static void syncGet(Context c, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        getSyncClient(c).get(url, params, responseHandler);
    }

    public static void asyncGet(Context c, String url, AsyncHttpResponseHandler responseHandler) {

        getAsyncClient(c).get(url, responseHandler);
    }

    public static void syncGet(Context c, String url, AsyncHttpResponseHandler responseHandler) {
        getSyncClient(c).get(url, responseHandler);
    }

    public static void asyncPost(Context c, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        getAsyncClient(c).post(url, params, responseHandler);
    }

    public static void asyncPost(Context c, String url, AsyncHttpResponseHandler responseHandler) {
        getAsyncClient(c).post(url, responseHandler);
    }

    public static void syncPost(Context c, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        getSyncClient(c).post(url, params, responseHandler);
    }

    public static void syncPost(Context c, String url, AsyncHttpResponseHandler responseHandler) {
        getSyncClient(c).post(url, responseHandler);
    }
}
