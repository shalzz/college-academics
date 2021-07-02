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
import com.shalzz.attendance.utils.Utils;
import com.tenmiles.helpstack.HSHelpStack;

import java.util.HashSet;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class MyApplication extends MultiDexApplication {

    private static ApplicationComponent mApplicationComponent;
    public static HSHelpStack helpStack;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Configuration config = Configuration.load(this);
        config.setEnabledReleaseStages(new HashSet<String>() {{
            add("production");
            add("development");
            add("testing");
        }});
        config.setMaxBreadcrumbs(100);
        if (!Utils.isRoboUnitTest()) {
            Bugsnag.start(this, config);
        }

        Timber.plant(new BugsnagTree());

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        FirebaseApp.initializeApp(this);

        helpStack = HSHelpStack.getInstance(this);
        helpStack.setOptions("support@8bitlabs.tech", R.xml.articles);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
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
