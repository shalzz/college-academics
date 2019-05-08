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

package com.shalzz.attendance.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

import com.shalzz.attendance.MyApplication
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper

import javax.inject.Inject

/**
 * A bound Service that instantiates the authenticator
 * when started.
 */
class AuthenticatorService : Service() {

    // Instance field that stores the authenticator object
    private var mAuthenticator: Authenticator? = null

    @Inject
    lateinit var mDataManager: DataManager

    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper

    override fun onCreate() {
        MyApplication.get(this).component.inject(this)
        mAuthenticator = Authenticator(mDataManager, mPreferencesHelper, this)
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    override fun onBind(intent: Intent): IBinder? {
        return mAuthenticator!!.iBinder
    }
}