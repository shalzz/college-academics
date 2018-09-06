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

package com.shalzz.attendance.data.remote.interceptor;

import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.remote.DataAPI;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class AuthInterceptor implements Interceptor{

    private PreferencesHelper preferencesManager;

    @Inject
    public AuthInterceptor(PreferencesHelper preferencesManager) {
        this.preferencesManager = preferencesManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest);
        }

        String id = preferencesManager.getUserId();
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("User Auth token cannot be empty");
        }

        String token = preferencesManager.getToken();
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("GCM Registration token cannot be empty");
        }

        Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + id)
                .header("x-reg-id", token)
                .build();
        return chain.proceed(newRequest);
    }
}
