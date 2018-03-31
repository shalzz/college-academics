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

package com.shalzz.attendance.data.local;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.ui.main.MainActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Credentials;
import timber.log.Timber;

@SuppressLint("CommitPrefEdits")
@Singleton
public class PreferencesHelper {

    public static final String PREF_FILE_NAME = "attendance_pref_file";

    private final SharedPreferences mPref;

    @Inject
    public PreferencesHelper(@ApplicationContext Context context) {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void clear() {
        mPref.edit().clear().apply();
    }

    public boolean shouldShowcase(int id) {
        return !mPref.getBoolean(String.valueOf(id), false);
    }

    public void showcasedView(int id) {
        mPref.edit().putBoolean(String.valueOf(id), true).apply();
    }

	public boolean getLoginStatus() {
		return mPref.getBoolean("LOGGEDIN", false);
	}

	public String getBasicAuthCredentials() {
        return Credentials.basic(mPref.getString("USERNAME", null),
                mPref.getString("PASSWORD", null));
	}

	/**
	 * Saves the user details in shared preferences and sets login status to true.
	 * @param username Username
	 * @param password Password
	 */
	public void saveUser(String username, String password) {
		Timber.i("Setting LOGGEDIN pref to true");
		SharedPreferences.Editor editor = mPref.edit();
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
		SharedPreferences.Editor editor = mPref.edit();
        editor.remove(MainActivity.PREFERENCE_ACTIVATED_FRAGMENT);
		editor.putBoolean("LOGGEDIN", false);
		editor.remove("USERNAME");
		editor.remove("PASSWORD");
		editor.commit();
	}
}
