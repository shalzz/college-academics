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

package com.shalzz.attendance;

import android.content.Context;
import android.content.SharedPreferences;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.google.firebase.FirebaseApp;
import com.shalzz.attendance.injection.component.ApplicationComponent;
import com.shalzz.attendance.injection.component.DaggerApplicationComponent;
import com.shalzz.attendance.injection.module.ApplicationModule;
import com.shalzz.attendance.utils.BugsnagTree;
import com.zoho.deskportalsdk.DeskConfig;
import com.zoho.deskportalsdk.ZohoDeskPortalSDK;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class MyApplication extends MultiDexApplication {

    private static ApplicationComponent mApplicationComponent;
    public static ZohoDeskPortalSDK deskInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        Configuration config = new Configuration(getString(R.string.bugsnag_api));
        config.setMaxBreadcrumbs(100);
        config.setAutomaticallyCollectBreadcrumbs(false);
        config.setAutoCaptureSessions(false);
        Bugsnag.init(this, config);
        Bugsnag.setNotifyReleaseStages("production", "development", "testing");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        FirebaseApp.initializeApp(this);

        ZohoDeskPortalSDK.Logger.enableLogs();
        DeskConfig deskConfig = new DeskConfig.Builder().build();
        deskInstance = ZohoDeskPortalSDK.getInstance(this);
        deskInstance.setThemeResource(R.style.deskTheme);
        deskInstance.initDesk(60002896708L,
                "0cf0e6f11763c00d387ee247ab64aed483f3768859c3ef35",
                ZohoDeskPortalSDK.DataCenter.IN,
                deskConfig);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.plant(new BugsnagTree());

        int nightMode = Integer.parseInt(sharedPref.getString(
                getString(R.string.pref_key_day_night), "1"));
        //noinspection WrongConstant
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static MyApplication get(Context context) {
        return (MyApplication) context.getApplicationContext();
    }

    public ApplicationComponent getComponent() {
        if (mApplicationComponent == null) {
            mApplicationComponent = DaggerApplicationComponent.builder()
                    .applicationModule(new ApplicationModule(this))
                    .build();
        }
        return mApplicationComponent;
    }

    // Needed to replace the component with a test specific one
    public void setComponent(ApplicationComponent applicationComponent) {
        mApplicationComponent = applicationComponent;
    }
}
