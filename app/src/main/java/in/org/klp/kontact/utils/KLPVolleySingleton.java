package in.org.klp.kontact.utils;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by bibhas on 6/19/16.
 *
 * More about RequestQueue and Singletons - https://developer.android.com/training/volley/requestqueue.html
 */
public class KLPVolleySingleton {
    private static KLPVolleySingleton mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    private KLPVolleySingleton(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized KLPVolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new KLPVolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}