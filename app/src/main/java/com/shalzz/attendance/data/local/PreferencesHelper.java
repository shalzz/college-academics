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

@SuppressLint("CommitPrefEdits")
@Singleton
public class PreferencesHelper {

    private static final String PREF_FILE_NAME = "attendance_pref_file";

    private final SharedPreferences mPref;

    @Inject
    public PreferencesHelper(@ApplicationContext Context context) {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void clear() {
        mPref.edit().clear().apply();
    }

	public boolean getLoginStatus() {
		return mPref.getBoolean("LOGGEDIN", false);
	}

	public String getUserId() {
        return mPref.getString("USERNAME", null);
    }

	/**
	 * Saves the user details in shared preferences and sets login status to true.
	 * @param username Username
	 */
	public void saveUser(String username) {
		SharedPreferences.Editor editor = mPref.edit();
		editor.putBoolean("LOGGEDIN", true);
		editor.putString("USERNAME", username);
		editor.commit();
	}

	/**
	 * Removes the user details from the shared preferences and sets login status to false.
	 */
	public void removeUser() {
		SharedPreferences.Editor editor = mPref.edit();
        editor.remove(MainActivity.Companion.getPREFERENCE_ACTIVATED_FRAGMENT());
		editor.putBoolean("LOGGEDIN", false);
		editor.remove("USERNAME");
		editor.commit();
	}

    public void saveToken(String token) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString("REGID", token);
        editor.commit();
    }

    public String getToken() {
        return mPref.getString("REGID", null);
    }
}
