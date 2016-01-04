/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
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
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.shalzz.attendance.R;
import com.shalzz.attendance.wrapper.MyPreferencesManager;

public class SplashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean optIn = sharedPref.getBoolean(getString(R.string.pref_key_ga_opt_in), true);
		GoogleAnalytics.getInstance(this).setAppOptOut(!optIn);

        // Set all default values once for this application
        try {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            PreferenceManager.setDefaultValues(this, R.xml.pref_proxy, false);
        } catch (ClassCastException e) {
			MyPreferencesManager.removeDefaultSharedPreferences();
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
            PreferenceManager.setDefaultValues(this, R.xml.pref_proxy, true);
        }

		boolean loggedin = MyPreferencesManager.getLoginStatus();
		
		if(!loggedin)
			startActivity(new Intent(SplashActivity.this, LoginActivity.class));
		else
			startActivity(new Intent(SplashActivity.this, MainActivity.class));
		
		finish();
	}
}
