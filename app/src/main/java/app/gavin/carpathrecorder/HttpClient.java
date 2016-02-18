package app.gavin.carpathrecorder;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

/**
 * Created by Gavin on 2016/2/18.
 */
public class HttpClient {
    private static AsyncHttpClient mAsyncClient = new AsyncHttpClient();
    private static SyncHttpClient mSyncClient = new SyncHttpClient();

    public static void asyncGet(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mAsyncClient.get(url, params, responseHandler);
    }

    public static void asyncPost(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mAsyncClient.post(url, params, responseHandler);
    }

    public static void syncGet(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mSyncClient.get(url, params, responseHandler);
    }

    public static void syncPost(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        mSyncClient.post(url, params, responseHandler);
    }
}
