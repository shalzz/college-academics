package com.shalzz.attendance.data.remote.interceptor;

import android.content.Context;
import android.support.annotation.NonNull;

import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.utils.NetworkUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheControlInterceptor implements Interceptor {
    private Context mContext;

    @Inject
    public CacheControlInterceptor(@ApplicationContext Context context) {
        mContext = context;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (NetworkUtil.isNetworkConnected(mContext)) {
            Response originalResponse = chain.proceed(chain.request());
            int maxAge = 60; // read from cache for 1 minute
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=" + maxAge)
                    .build();

        } else {
            Request request = chain.request();
            // only for the 'verify' api route
            if (request.url().encodedPath().equals("/api/v1/verify")) {
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
}
