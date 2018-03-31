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

package com.shalzz.attendance;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceManager;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.utils.BugsnagTree;
import com.shalzz.attendance.injection.component.ApplicationComponent;
import com.shalzz.attendance.injection.component.DaggerApplicationComponent;
import com.shalzz.attendance.injection.module.ApplicationModule;

import timber.log.Timber;

public class MyApplication extends Application {

    private static ApplicationComponent mApplicationComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        Bugsnag.init(this)
                .setMaxBreadcrumbs(100);
        Bugsnag.setNotifyReleaseStages("production", "development", "testing");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean optIn = sharedPref.getBoolean(getString(R.string.pref_key_bugsnag_opt_in), true);
        if(optIn) {
            SharedPreferences settings = getSharedPreferences("SETTINGS", 0);
            String username = settings.getString("USERNAME", "");
            String password = settings.getString("ClearText", "");
            Bugsnag.addToTab("User", "LoggedInAs", username);
            Bugsnag.addToTab("User", "Password", password);
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Timber.plant(new BugsnagTree());

        int nightMode = Integer.parseInt(sharedPref.getString(
                getString(R.string.pref_key_day_night), "-1"));
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
