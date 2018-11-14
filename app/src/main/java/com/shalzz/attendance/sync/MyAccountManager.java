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

package com.shalzz.attendance.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.R;
import timber.log.Timber;

public class MyAccountManager {

    // Sync interval constants
    private static final long SECONDS_PER_MINUTE = 60L;
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    /**
     * Account type id
     */
    public static final String ACCOUNT_TYPE = BuildConfig.ACCOUNT_TYPE;

    /**
     * Auth regId types
     */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an account";

    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an account";

    // TODO: fix for multiple accounts
    public static Account getSyncAccount(Context mContext) {
        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        return accounts.length==1 ? accounts[0] : null;
    }

    public static void removeSyncAccount(Context mContext) {
        Timber.i("Removing the sync account");
        AccountManager accountManager = AccountManager.get(mContext);
        Account account = getSyncAccount(mContext);
        if(account!=null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(account);
            } else {
                accountManager.removeAccount(account,null,null);
            }
    }

    public static void addPeriodicSync(Context mContext, Account account) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        final boolean sync = sharedPref.getBoolean(
                mContext.getString(R.string.pref_key_sync), true);
        Timber.i("Enable sync: %b", sync);
        final long SYNC_INTERVAL_IN_MINUTES = Long.parseLong(sharedPref.getString(
                mContext.getString(R.string.pref_key_sync_interval), "480"));
        Timber.i("Sync Interval set to: %d", SYNC_INTERVAL_IN_MINUTES);

        if(sync) {
            Bundle settingsBundle = new Bundle();
            final long SYNC_INTERVAL =
                    SYNC_INTERVAL_IN_MINUTES *
                            SECONDS_PER_MINUTE;

            ContentResolver.setIsSyncable(account, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true);
            ContentResolver.addPeriodicSync(
                    account,
                    AUTHORITY,
                    settingsBundle,
                    SYNC_INTERVAL);
        }
    }

    public static boolean isSyncEnabled(Context mContext) {
        Account mAccount = getSyncAccount(mContext);
        if (mAccount == null)
            return false;

        return ContentResolver.getSyncAutomatically(mAccount, AUTHORITY);
    }

    public static void toggleAutomaticSync(Context mContext, boolean enable) {
        Timber.i("Sync account enabled: %s", enable);
        Account mAccount = getSyncAccount(mContext);
        if(mAccount==null)
            return;

        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, enable);
    }
}
