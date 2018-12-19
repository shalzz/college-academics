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

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.AccountManager.KEY_BOOLEAN_RESULT
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import com.shalzz.attendance.sync.MyAccountManager.AUTHTOKEN_TYPE_FULL_ACCESS
import com.shalzz.attendance.sync.MyAccountManager.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL
import com.shalzz.attendance.sync.MyAccountManager.AUTHTOKEN_TYPE_READ_ONLY
import com.shalzz.attendance.sync.MyAccountManager.AUTHTOKEN_TYPE_READ_ONLY_LABEL
import com.shalzz.attendance.ui.login.AuthenticatorActivity

class Authenticator(private val mContext: Context) : AbstractAccountAuthenticator(mContext) {

    @Throws(NetworkErrorException::class)
    override fun addAccount(
            response: AccountAuthenticatorResponse,
            accountType: String,
            authTokenType: String?,
            requiredFeatures: Array<String>?,
            options: Bundle?): Bundle? {

        val intent = Intent(mContext, AuthenticatorActivity::class.java)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType)
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
        intent.putExtra(AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthToken(
            response: AccountAuthenticatorResponse,
            account: Account,
            authTokenType: String,
            options: Bundle): Bundle {

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(AUTHTOKEN_TYPE_READ_ONLY)
                && !authTokenType.equals(AUTHTOKEN_TYPE_FULL_ACCESS)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType")
            return result
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        val am = AccountManager.get(mContext)

        val authToken = am.peekAuthToken(account, authTokenType)

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            val result = Bundle()
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
            return result
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        val intent = Intent(mContext, AuthenticatorActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type)
        intent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(authTokenType: String): String {
        return when {
            AUTHTOKEN_TYPE_FULL_ACCESS.equals(authTokenType) -> AUTHTOKEN_TYPE_FULL_ACCESS_LABEL
            AUTHTOKEN_TYPE_READ_ONLY.equals(authTokenType) -> AUTHTOKEN_TYPE_READ_ONLY_LABEL
            else -> "$authTokenType (Label)"
        }
    }

    override fun confirmCredentials(
            r: AccountAuthenticatorResponse,
            account: Account,
            bundle: Bundle): Bundle? {
        return null
    }
    override fun editProperties(
            r: AccountAuthenticatorResponse, s: String): Bundle? {
        return null
    }

    override fun updateCredentials(
            r: AccountAuthenticatorResponse,
            account: Account,
            s: String, bundle: Bundle): Bundle? {
        return null
    }

    override fun hasFeatures(
            r: AccountAuthenticatorResponse,
            account: Account, strings: Array<String>): Bundle {
        val result = Bundle()
        result.putBoolean(KEY_BOOLEAN_RESULT, false)
        return result
    }

    private fun isAccountAvailable(account: Account, accountManager: AccountManager): Boolean {
        val availableAccounts = accountManager.getAccountsByType(account.type)
        for (availableAccount in availableAccounts) {
            if (account.name == availableAccount.name && account.type == availableAccount.type) {
                return true
            }
        }
        return false
    }
}
