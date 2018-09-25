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

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.data.remote.RxJava2ErrorCallAdapterFactory;
import com.shalzz.attendance.data.remote.interceptor.AuthInterceptor;
import com.shalzz.attendance.data.remote.interceptor.CacheControlInterceptor;
import com.shalzz.attendance.data.remote.interceptor.HeaderInterceptor;
import com.shalzz.attendance.data.remote.interceptor.LoggingInterceptor;
import com.shalzz.attendance.injection.ApplicationContext;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetworkModule {

    @Provides @Singleton
    static Gson provideGson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd")
                .create();
    }

    @Provides @Singleton @NonNull
    static OkHttpClient provideClient(PreferencesHelper preferences,
                                      @ApplicationContext Context context) {
        //setup cache
        File httpCacheDirectory = new File(context.getCacheDir(), "responses");
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);

        final OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(new HeaderInterceptor())
                .addInterceptor(new AuthInterceptor(preferences))
                .addNetworkInterceptor(new CacheControlInterceptor(context))
                .addNetworkInterceptor(new LoggingInterceptor());

        return okHttpBuilder.build();
    }

    @Provides @Singleton @NonNull
    static DataAPI provideApi(@NonNull OkHttpClient okHttpClient,
                              @NonNull Gson gson,
                              @ApplicationContext Context context) {
        return new Retrofit.Builder()
                .baseUrl(DataAPI.ENDPOINT)
                .addCallAdapterFactory(RxJava2ErrorCallAdapterFactory.create(context))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .validateEagerly(BuildConfig.DEBUG) // Fail early: check Retrofit configuration at creation time in Debug build.
                .build()
                .create(DataAPI.class);
    }
}
