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

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.os.Bundle
import com.shalzz.attendance.ui.base.BaseActivity

/**
 * Base class for implementing an Activity that is used to help implement an
 * AbstractAccountAuthenticator. If the AbstractAccountAuthenticator needs to use an activity
 * to handle the request then it can have the activity extend AccountAuthenticatorActivity.
 * The AbstractAccountAuthenticator passes in the response to the intent using the following:
 * <pre>
 * intent.putExtra([AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE], response);
</pre> *
 * The activity then sets the result that is to be handed to the response via
 * [.setAccountAuthenticatorResult].
 * This result will be sent as the result of the request when the activity finishes. If this
 * is never set or if it is set to null then error [AccountManager.ERROR_CODE_CANCELED]
 * will be called on the response.
 */
@SuppressLint("Registered")
open class AccountAuthenticatorActivity : BaseActivity() {
    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    fun setAccountAuthenticatorResult(result: Bundle) {
        mResultBundle = result
    }

    /**
     * Retreives the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param icicle the save instance data of this Activity, may be null
     */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        mAccountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse!!.onRequestContinued()
        }
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    override fun finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse!!.onResult(mResultBundle)
            } else {
                mAccountAuthenticatorResponse!!.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled")
            }
            mAccountAuthenticatorResponse = null
        }
        super.finish()
    }
}
