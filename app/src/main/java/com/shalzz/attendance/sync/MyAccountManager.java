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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
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
     * Auth token types
     */
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an account";

    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an account";

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    private static Account CreateSyncAccount(Context context, String accountName) {
        Timber.i("Creating a sync account");
        // Create the account type and default account
        Account newAccount = new Account(accountName, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
		/*
		 * Add the account and account type, no password or user data
		 * If successful, return the Account object, otherwise report an error.
		 */
        if (!accountManager.addAccountExplicitly(newAccount, null, null)) {
			/*
			 * The account exists or some other error occurred. Log this, report it,
			 * or handle it internally.
			 */
            Timber.e("Account already exits!");
        }
        return newAccount;
    }

    private static Account getSyncAccount(Context mContext) {
        AccountManager accountManager = AccountManager.get(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.GET_ACCOUNTS) !=
                PackageManager.PERMISSION_GRANTED) {
            Timber.d("GET_ACCOUNTS permission denied");
            return null;
        }
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
	
	public static void addPeriodicSync(Context mContext,String accountName) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
		final boolean sync = sharedPref.getBoolean(
                mContext.getString(R.string.pref_key_sync), true);
		Timber.i("Enable sync: %b", sync);
		final long SYNC_INTERVAL_IN_MINUTES = Long.parseLong(sharedPref.getString(
                mContext.getString(R.string.pref_key_sync_interval), "480"));
        Timber.i("Sync Interval set to: %d", SYNC_INTERVAL_IN_MINUTES);
		
		Account mAccount = getSyncAccount(mContext);
		
		if(mAccount==null)
			mAccount = CreateSyncAccount(mContext,accountName);	
		
		if(sync) 
		{	// Create the dummy account
			Bundle settingsBundle = new Bundle();
			final long SYNC_INTERVAL =
					SYNC_INTERVAL_IN_MINUTES *
					SECONDS_PER_MINUTE;

			ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
			ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
			ContentResolver.addPeriodicSync(
					mAccount,
					AUTHORITY,
					settingsBundle,
					SYNC_INTERVAL);
		}
	}

	public static void enableAutomaticSync(Context mContext,String accountName) {
        Timber.i("Sync account enabled");
        Account mAccount = getSyncAccount(mContext);
        if(mAccount==null)
            mAccount = CreateSyncAccount(mContext,accountName);

        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
	}

    public static void disableAutomaticSync(Context mContext,String accountName) {
        Timber.i("Sync account disabled");
        Account mAccount = getSyncAccount(mContext);
        if(mAccount==null)
            mAccount = CreateSyncAccount(mContext,accountName);

        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, false);
	}
}
