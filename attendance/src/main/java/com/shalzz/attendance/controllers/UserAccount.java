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

package com.shalzz.attendance.controllers;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.activity.LoginActivity;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.data.model.remote.User;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MySyncManager;

import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class UserAccount {

    private final MyPreferencesManager preferencesManager;
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
    public UserAccount(Context context,
                       DataAPI api) {
        mContext = context;
        mAPI = api;
        preferencesManager = new MyPreferencesManager(mContext);
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
                if(response.isSuccessful()) {
                    User user = response.body();
                    preferencesManager.saveUser(user.sap_id(), user.password());
                    MySyncManager.addPeriodicSync(mContext, user.sap_id());
                    DatabaseHandler db = new DatabaseHandler(mContext);
                    db.addUser(user);
                    db.close();

                    misc.dismissProgressDialog();
                    Intent ourIntent = new Intent(mContext, MainActivity.class);
                    mContext.startActivity(ourIntent);
                    ((Activity) mContext).finish();
                } else {
                    showError(response.raw().message());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                showError(t.getLocalizedMessage());
                t.printStackTrace();
            }
        });
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
        DatabaseHandler db = new DatabaseHandler(mContext);
        db.resetTables();

        // Remove Sync Account
        MySyncManager.removeSyncAccount(mContext);

        // Cancel a notification if it is shown.
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0 /** timetable changed notification id */);

        // Destroy current activity and start Login Activity
        Intent ourIntent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(ourIntent);
        ((Activity) mContext).finish();
    }
}
