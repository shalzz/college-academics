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

package com.shalzz.attendance.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.ui.main.MainActivity

import javax.inject.Inject
import javax.inject.Singleton

import timber.log.Timber

@SuppressLint("CommitPrefEdits")
@Singleton
class PreferencesHelper @Inject
constructor(@ApplicationContext context: Context) {

    private val mPref: SharedPreferences

    val loginStatus: Boolean
        get() = mPref.getBoolean("LOGGEDIN", false)

    val userId: String?
        get() = mPref.getString("USERNAME", null)

    val token: String?
        get() = mPref.getString("REGID", null)

    init {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun clear() {
        mPref.edit().clear().apply()
    }

    /**
     * Saves the user details in shared preferences and sets login status to true.
     * @param username Username
     */
    fun saveUser(username: String) {
        val editor = mPref.edit()
        editor.putBoolean("LOGGEDIN", true)
        editor.putString("USERNAME", username)
        editor.commit()
    }

    /**
     * Removes the user details from the shared preferences and sets login status to false.
     */
    fun removeUser() {
        val editor = mPref.edit()
        editor.remove(MainActivity.PREFERENCE_ACTIVATED_FRAGMENT)
        editor.putBoolean("LOGGEDIN", false)
        editor.remove("USERNAME")
        editor.commit()
    }

    fun saveToken(token: String) {
        val editor = mPref.edit()
        editor.putString("REGID", token)
        editor.commit()
    }

    fun upgradePrefsIfNecessary(context: Context) {
        var version = ""
        try {
            version = context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        when (version)  {
            "v3.1.10" -> {
                if(mPref.getBoolean("update_required", true)) {
                    val editor = mPref.edit()
                    editor.putBoolean("LOGGEDIN", false)
                    editor.putBoolean("update_required", false)
                    editor.commit()
                }

                Timber.d("Upgrading preferences to: %s", version)
            }
            else -> Timber.d("Preference upgrade not required.")
        }
    }

    companion object {

        private val PREF_FILE_NAME = "attendance_pref_file"
    }
}
