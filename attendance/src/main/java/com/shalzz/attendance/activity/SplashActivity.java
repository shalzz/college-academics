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

package com.shalzz.attendance.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.wrapper.MyPreferencesManager;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class SplashActivity extends BaseActivity {

    @Inject
    MyPreferencesManager preferencesManager;

    @Inject
    @Named("app")
    Tracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
		Bugsnag.setContext("SplashActivity");

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean optIn = sharedPref.getBoolean(getString(R.string.pref_key_ga_opt_in), true);
		GoogleAnalytics.getInstance(this).setAppOptOut(!optIn);
        Timber.i("Opted out of Google Analytics: %s", !optIn);

        mTracker.send(new HitBuilders.ScreenViewBuilder()
                        .setCustomDimension(Miscellaneous.CUSTOM_DIMENSION_THEME,
                                sharedPref.getString(getString(R.string.pref_key_day_night), "-1"))
                        .setCustomDimension(Miscellaneous.CUSTOM_DIMENSION_PROXY,""+
                                sharedPref.getBoolean(getString(R.string.pref_key_use_proxy),false))
                        .build());

                // Set all default values once for this application
        try {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            PreferenceManager.setDefaultValues(this, R.xml.pref_proxy, false);
        } catch (ClassCastException e) {
	        Bugsnag.notify(e, Severity.INFO);
            preferencesManager.removeDefaultSharedPreferences();
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            PreferenceManager.setDefaultValues(this, R.xml.pref_proxy, true);
        }

		boolean loggedin = preferencesManager.getLoginStatus();

        Intent intent;
		if(!loggedin)
            intent = new Intent(SplashActivity.this, LoginActivity.class);
		else
            intent = new Intent(SplashActivity.this, MainActivity.class);

        startActivity(intent);
		finish();
	}
}
