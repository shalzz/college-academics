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

package com.shalzz.attendance.ui.splash

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Severity
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.sync.MyAccountManager
import com.shalzz.attendance.ui.base.BaseActivity
import com.shalzz.attendance.ui.main.MainActivity
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Named

class SplashActivity : BaseActivity() {

    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    @Inject
    lateinit var mPresenter: SplashPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        Bugsnag.setContext("SplashActivity")
        mPreferencesHelper.upgradePrefsIfNecessary(this)

        mPresenter.getToken(getString(R.string.onedu_gcmSenderId))
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val optIn = sharedPref.getBoolean(getString(R.string.pref_key_ga_opt_in), true)
        mTracker.setAnalyticsCollectionEnabled(optIn)
        Timber.i("Opted In to Google Analytics: %s", optIn)

        // Set all default values once for this application
        try {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        } catch (e: ClassCastException) {
            Bugsnag.notify(e, Severity.INFO)
            PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply()
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        }

        val loggedIn = mPreferencesHelper.loginStatus
        if (loggedIn) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
        else {
            AccountManager.get(this).addAccount(
                MyAccountManager.ACCOUNT_TYPE,
                MyAccountManager.AUTHTOKEN_TYPE_READ_ONLY,
                null,
                null,
                this,
                { future ->
                    try {
                        val bundle = future.result
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        finish()
                    }
                },
                null
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.d("requestCode: %d, resultCode: %d, data: %s", requestCode, resultCode, data!!.extras)
    }
}
