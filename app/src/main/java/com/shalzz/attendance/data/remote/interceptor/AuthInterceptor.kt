/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
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

import android.util.Base64
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.remote.DataAPI
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject
constructor(private val preferencesManager: PreferencesHelper) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        if (originalRequest.url().encodedPath().startsWith(DataAPI.API_VERSION + "login/")
           || originalRequest.url().encodedPath().startsWith(DataAPI.API_VERSION + "verify-otp/")) {
            return chain.proceed(originalRequest)
        }

        val id = preferencesManager.userId
        val token = preferencesManager.token
        val auth = Base64.encodeToString("$id:$token".toByteArray(), Base64.DEFAULT)
        if (id == null || token == null || id.isEmpty()) {
            throw RuntimeException("User Auth regId cannot be empty")
        }

        val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Basic $auth")
                .build()
        return chain.proceed(newRequest)
    }
}
