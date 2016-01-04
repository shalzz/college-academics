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

package com.shalzz.attendance.wrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;

public class MyPreferencesManager {

	/**
	 * The activity context.
	 */
	private static Context mContext = MyVolley.getAppContext();

	private static String mTag  = "MyPreferencesManager";
	
	public static void setLastSyncTime() {
		Date now = new Date();
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong("REFRESH_TIME", now.getTime());
		editor.commit();
	}
	
	public static long getLastSyncTime() {
        Time now = new Time();
        now.setToNow();
        Long now_L = now.toMillis(false);
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", Context.MODE_MULTI_PROCESS);
		Long last_sync = settings.getLong("REFRESH_TIME", now_L);
		return (now_L-last_sync)/(1000*60*60); // convert milliseconds to hours
	}

	/**
	 * Gets the login status from the preferences
	 * @return true if logged in else false
	 */
	public static boolean getLoginStatus() {

		Log.i(mTag, "Getting Logged in state.");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		boolean loggedin = settings.getBoolean("LOGGEDIN" + mContext.getResources().getString(R.string.user_version), false);
		Log.d(mTag, "Logged in state: " + loggedin);
		return loggedin;
	}

	public static String getUser() {
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);

		return String.format("%s:%s", settings.getString("USERNAME", null)
				, settings.getString("PASSWORD", null));
	}

	/**
	 * Saves the user details in shared preferences and sets login status to true.
	 * @param username Username
	 * @param password Password
	 */
	public static void saveUser(String username, String password) {
		Log.i(mTag, "Setting LOGGEDIN pref to true");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("LOGGEDIN"+mContext.getResources().getString(R.string.user_version), true);
		editor.putString("USERNAME", username);
		editor.putString("PASSWORD", password);
		editor.commit();
	}

	/**
	 * Removes the user details from the shared preferences and sets login status to false.
	 */
	public static void removeUser() {
		Log.i(mTag, "Setting LOGGEDIN pref to false");
		SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
		SharedPreferences.Editor editor = settings.edit();
        editor.remove(MainActivity.PREFERENCE_ACTIVATED_FRAGMENT);
		editor.putBoolean("LOGGEDIN"+mContext.getResources().getString(R.string.user_version), false);
		editor.remove("USERNAME");
		editor.remove("PASSWORD");
		editor.commit();
	}

    /**
     * Checks weather this is the first time the app is launched or not.
     * @return True or False
     */
    public static boolean isFirstLaunch(String tag) {
        SharedPreferences settings = mContext.getSharedPreferences("LAUNCH", 0);
        return settings.getBoolean("FIRSTLAUNCH"+tag, true);
    }

    /**
     * Sets the first launch to false.
     */
    public static void setFirstLaunch(String tag) {
        SharedPreferences settings = mContext.getSharedPreferences("LAUNCH", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("FIRSTLAUNCH"+tag, false);
        editor.commit();
    }

    public static void removeSettings() {
        removeDefaultSharedPreferences();
        SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }
    
    public static void removeDefaultSharedPreferences() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }
}
