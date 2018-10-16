package com.shalzz.attendance.data.remote.interceptor

import android.content.Context
import com.shalzz.attendance.data.remote.DataAPI
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.utils.NetworkUtil
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CacheControlInterceptor @Inject
constructor(@param:ApplicationContext private val mContext: Context) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (NetworkUtil.isNetworkConnected(mContext)) {
            // Do not cache the '/me' api route
            return if (request.url().encodedPath() == DataAPI.API_VERSION + "me") {
                val originalResponse = chain.proceed(request)
                originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=0")
                        .build()
            } else {
                Timber.d("Caching: %s", request.url().encodedPath())
                val originalResponse = chain.proceed(request)
                val maxAge = 60 // read from cache for 1 minute
                originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=$maxAge")
                        .build()
            }
            // only for the 'verify' api route
        } else if (request.url().encodedPath() == DataAPI.API_VERSION + "verify") {
            val cacheControl = CacheControl.Builder()
                    .onlyIfCached()
                    .maxStale(7, TimeUnit.DAYS)
                    .build()

            request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
        }
        return chain.proceed(request)
    }
}
