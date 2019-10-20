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

import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.remote.DataAPI
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject
constructor(private val preferencesManager: PreferencesHelper) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.header("x-auth-token") != null ||
                originalRequest.url().encodedPath().startsWith(DataAPI.API_VERSION + "me/login") ||
                !originalRequest.url().encodedPath().startsWith(DataAPI.API_VERSION + "me")) {
            Timber.d("skipping auth interceptor")
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
                .header("x-auth-token", preferencesManager.token)
                .build()
        return chain.proceed(newRequest)
    }
}
