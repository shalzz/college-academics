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

package com.shalzz.attendance.wrapper;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.model.remote.GsonAdaptersRemote;
import com.shalzz.attendance.data.network.AuthInterceptor;
import com.shalzz.attendance.data.network.DataAPI;
import com.shalzz.attendance.data.network.HeaderInterceptor;
import com.shalzz.attendance.data.network.LoggingInterceptor;

import java.util.HashMap;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Wrapper class for Volley which provides a singleton instance.
 * @author shalzz
 *
 */
public class MyVolley extends Application {

    /**
     * Application Context.
     */
    private static  Context mContext;

    /**
     * A singleton instance of the application class for easy access in other places
     */
    private static MyVolley sInstance;

    private static Gson mGson;
    private static OkHttpClient mOkHttpClient;
    private static DataAPI mAPI;

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the singleton
        sInstance = this;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int nightMode = Integer.parseInt(sharedPref.getString(
                getString(R.string.pref_key_day_night), "-1"));
        //noinspection WrongConstant
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /**
     * @return ApplicationController singleton instance
     */
    public static synchronized MyVolley getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        if(mContext == null)
            mContext = sInstance.getApplicationContext();
        return mContext;
    }

    public static Resources getMyResources() {
        return MyVolley.getAppContext().getResources();
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            if(trackerId == TrackerName.APP_TRACKER) {
                Tracker t = analytics.newTracker(R.xml.app_tracker);
                mTrackers.put(trackerId, t);
            }
        }
        return mTrackers.get(trackerId);
    }

    public static Gson provideGson() {
        if(mGson == null) {
            mGson = new GsonBuilder()
                    .registerTypeAdapterFactory(new GsonAdaptersRemote())
                    .setDateFormat("yyyy-MM-dd")
                    .create();
        }
        return mGson;
    }

    @NonNull
    public static OkHttpClient provideClient() {
        if (mOkHttpClient == null) {
            final OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder()
                    .addInterceptor(new LoggingInterceptor())
                    .addInterceptor(new HeaderInterceptor())
                    .addInterceptor(new AuthInterceptor())
                    .proxyAuthenticator(MyPreferencesManager.getProxyCredentials())
                    .proxySelector(Miscellaneous.getProxySelector());
            mOkHttpClient = okHttpBuilder.build();
        }
        return mOkHttpClient;
    }

    public static DataAPI provideApi(@NonNull OkHttpClient okHttpClient, @NonNull Gson gson) {
        if(mAPI == null) {
            mAPI = new Retrofit.Builder()
                    .baseUrl(DataAPI.ENDPOINT)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .validateEagerly(BuildConfig.DEBUG) // Fail early: check Retrofit configuration at creation time in Debug build.
                    .build()
                    .create(DataAPI.class);
        }
        return mAPI;
    }
}
