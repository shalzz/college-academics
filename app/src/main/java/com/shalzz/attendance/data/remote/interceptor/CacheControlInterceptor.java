package com.shalzz.attendance.data.remote.interceptor;

import android.content.Context;

import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.utils.NetworkUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class CacheControlInterceptor implements Interceptor {
    private Context mContext;

    @Inject
    public CacheControlInterceptor(@ApplicationContext Context context) {
        mContext = context;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (NetworkUtil.isNetworkConnected(mContext)) {
            // Do not cache the '/me' api route
            if (request.url().encodedPath().equals(DataAPI.Companion.getAPI_VERSION() + "me")) {
                Response originalResponse = chain.proceed(request);
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=0")
                        .build();
            }
            else {
                Timber.d("Caching: %s",request.url().encodedPath());
                Response originalResponse = chain.proceed(request);
                int maxAge = 60; // read from cache for 1 minute
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            }
        // only for the 'verify' api route
        } else if (request.url().encodedPath().equals(DataAPI.Companion.getAPI_VERSION() + "verify")) {
            CacheControl cacheControl = new CacheControl.Builder()
                    .onlyIfCached()
                    .maxStale(7, TimeUnit.DAYS)
                    .build();

            request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build();
        }
        return chain.proceed(request);
    }
}
