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

package com.shalzz.attendance.ui.login;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.data.local.DbOpenHelper;
import com.shalzz.attendance.R;
import com.shalzz.attendance.injection.ActivityContext;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.wrapper.MySyncManager;

import javax.inject.Inject;

import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class UserAccount {

    private final PreferencesHelper preferencesManager;
    private final Miscellaneous misc;
    private final DataAPI mAPI;

    /**
     * The activity context used to Log the user from
     */
    private Context mContext;
    private Call<User> call;

    /**
     * Constructor to set the Activity context.
     * @param context Context
     */
    @Inject
    public UserAccount(@ActivityContext Context context,
                       DataAPI api) {
        mContext = context;
        mAPI = api;
        preferencesManager = new PreferencesHelper(mContext);
        misc = new Miscellaneous(context);
    }

    /**
     * Sends the login request and saves the user details.
     * @param username Username
     * @param password Password
     */
    public void Login(final String username, final String password) {

        String creds = Credentials.basic(username,Miscellaneous.md5(password));
        misc.showProgressDialog("Logging in...", false, pdCancelListener());

        call = mAPI.getUser(creds);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                User user = response.body();
                MySyncManager.addPeriodicSync(mContext, user.sap_id());

                configureBugsnag(password);

                misc.dismissProgressDialog();
                Intent ourIntent = new Intent(mContext, MainActivity.class);
                mContext.startActivity(ourIntent);
                ((Activity) mContext).finish();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                RetrofitException error = (RetrofitException) t;
                if (error.getKind() == RetrofitException.Kind.HTTP) {
                    showError(error.getMessage());
                }
                else if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                    String msg = mContext.getString(R.string.unexpected_error);
                    showError(msg);
                    Timber.e(t, msg);
                }
            }
        });
    }

    private void configureBugsnag(String password) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean optIn = sharedPref.getBoolean(mContext.getString(
                R.string.pref_key_bugsnag_opt_in), true);
        if(optIn) {
            Bugsnag.addToTab("User", "Password", password);
        }
        SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ClearText", password);
        editor.apply();
    }

    private void showError(String message) {
        misc.dismissProgressDialog();
        if(mContext == null)
            return;
        View view = ((Activity) mContext).findViewById(android.R.id.content);
        if(view != null)
            Miscellaneous.showSnackBar(view, message);
    }

    /**
     * Progress Dialog cancel Listener.
     * @return OnCancelListener
     */
    DialogInterface.OnCancelListener pdCancelListener() {
        return dialog -> {
            // Cancel all pending requests when user presses back button.
            call.cancel();
        };

    }

    /**
     * Sends the Logout request, clears the user details preferences and deletes all user attendance data.
     */
    public void Logout() {
        MainActivity.LOGGED_OUT = true;

        // Remove User Details from Shared Preferences.
        preferencesManager.removeUser();

        // Remove user Attendance data from database.
        DbOpenHelper db = new DbOpenHelper(mContext);
        db.resetTables();

        // Remove Sync Account
        MySyncManager.removeSyncAccount(mContext);

        // Cancel a notification if it is shown.
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0 /* timetable changed notification id */);

        // Destroy current activity and start Login Activity
        Intent ourIntent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(ourIntent);
        ((Activity) mContext).finish();
    }
}
