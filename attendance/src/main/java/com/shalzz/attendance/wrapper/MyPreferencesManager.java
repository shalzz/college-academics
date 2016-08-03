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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import timber.log.Timber;

@SuppressLint("CommitPrefEdits")
@Singleton
public class MyPreferencesManager {

	private static Context mContext;

	private static String mTag  = "MyPreferencesManager";

    @Inject
    public MyPreferencesManager(Context context) {
        mContext = context;
    }

	/**
	 * Gets the login status from the preferences
	 * @return true if logged in else false
	 */
	public boolean getLoginStatus() {

		Timber.i("Getting Logged in state.");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		boolean loggedin = settings.getBoolean("LOGGEDIN", false);
		Timber.i("Logged in state: %s", loggedin);
		return loggedin;
	}

	public String getBasicAuthCredentials() {
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
        return Credentials.basic(settings.getString("USERNAME", null),
                settings.getString("PASSWORD", null));
	}

    public Authenticator getProxyCredentials() {
        return new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
                final String username = sharedPref.getString(
                        mContext.getString(R.string.pref_key_proxy_username), "");
                final String password = sharedPref.getString(
                        mContext.getString(R.string.pref_key_proxy_password), "");
                return response.request().newBuilder()
                        .header("Proxy-Authorization", Credentials.basic(username,password))
                        .build();
            }
        };
    }

	/**
	 * Saves the user details in shared preferences and sets login status to true.
	 * @param username Username
	 * @param password Password
	 */
	public void saveUser(String username, String password) {
		Timber.i("Setting LOGGEDIN pref to true");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("LOGGEDIN", true);
		editor.putString("USERNAME", username);
		editor.putString("PASSWORD", password);
		editor.commit();
	}

	/**
	 * Removes the user details from the shared preferences and sets login status to false.
	 */
	public void removeUser() {
		Timber.i("Setting LOGGEDIN pref to false");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		SharedPreferences.Editor editor = settings.edit();
        editor.remove(MainActivity.PREFERENCE_ACTIVATED_FRAGMENT);
		editor.putBoolean("LOGGEDIN", false);
		editor.remove("USERNAME");
		editor.remove("PASSWORD");
		editor.commit();
	}

    public void removeSettings() {
        SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }
    
    public void removeDefaultSharedPreferences() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }
}
