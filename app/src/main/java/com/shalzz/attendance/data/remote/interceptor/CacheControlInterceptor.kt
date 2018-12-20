/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            return if (request.url().encodedPath().startsWith(DataAPI.API_VERSION + "login/")
                || request.url().encodedPath().startsWith(DataAPI.API_VERSION + "me/regid")
                || request.url().encodedPath().startsWith(DataAPI.API_VERSION + "verify-otp/")) {
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
