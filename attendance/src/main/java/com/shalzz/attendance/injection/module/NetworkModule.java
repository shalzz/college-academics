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

package com.shalzz.attendance.injection.module;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryanharter.auto.value.gson.AutoValueGsonTypeAdapterFactory;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.network.ErrorHandlingCallAdapterFactory;
import com.shalzz.attendance.network.interceptor.AuthInterceptor;
import com.shalzz.attendance.network.interceptor.HeaderInterceptor;
import com.shalzz.attendance.network.interceptor.LoggingInterceptor;
import com.shalzz.attendance.wrapper.MyPreferencesManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetworkModule {

    @Provides @Singleton
    static Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new AutoValueGsonTypeAdapterFactory())
                .setDateFormat("yyyy-MM-dd")
                .create();
    }

    @Provides @Singleton @NonNull
    static OkHttpClient provideClient(MyPreferencesManager preferences) {
        final OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .addInterceptor(new HeaderInterceptor())
                .addInterceptor(new AuthInterceptor(preferences))
                .addNetworkInterceptor(new LoggingInterceptor());

        if(Miscellaneous.useProxy()) {
            okHttpBuilder.proxyAuthenticator(preferences.getProxyCredentials());
            okHttpBuilder.proxySelector(Miscellaneous.getProxySelector());
        }

        return okHttpBuilder.build();
    }

    @Provides @Singleton @NonNull
    static DataAPI provideApi(@NonNull OkHttpClient okHttpClient,
                              @NonNull Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(DataAPI.ENDPOINT)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(ErrorHandlingCallAdapterFactory.create())
                .validateEagerly(BuildConfig.DEBUG) // Fail early: check Retrofit configuration at creation time in Debug build.
                .build()
                .create(DataAPI.class);
    }
}
