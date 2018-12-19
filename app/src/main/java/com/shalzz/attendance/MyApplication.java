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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.google.firebase.FirebaseApp;
import com.shalzz.attendance.injection.component.ApplicationComponent;
import com.shalzz.attendance.injection.component.DaggerApplicationComponent;
import com.shalzz.attendance.injection.module.ApplicationModule;
import com.shalzz.attendance.utils.BugsnagTree;
import timber.log.Timber;

public class MyApplication extends MultiDexApplication {

    private static ApplicationComponent mApplicationComponent;

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
