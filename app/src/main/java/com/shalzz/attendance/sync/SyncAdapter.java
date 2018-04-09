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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.ui.main.MainActivity;

import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    // Global variables
    private Context mContext;

    private final PreferencesHelper preferencesManager;
    private final DataAPI api;

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs, PreferencesHelper preferencesManager, DataAPI api) {
        super(context, autoInitialize, allowParallelSyncs);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
        mContext = context;
        this.preferencesManager = preferencesManager;
        this.api = api;
        Bugsnag.setContext("Sync Adapter");
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Timber.i("Running sync adapter");
    }

    /**
     * Notifies the user that their timetable has changed.
     */
    private void showNotification() {
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_stat_human)
                        .setLargeIcon(BitmapFactory.decodeResource(
                                mContext.getResources(),
                                R.mipmap.ic_launcher))
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                        .setContentTitle(mContext.getString(
                                R.string.notify_timetable_changed_title))
                        .setContentText(mContext.getString(
                                R.string.notify_timetable_changed_text));

        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.putExtra(MainActivity.LAUNCH_FRAGMENT_EXTRA, MainActivity
                .Fragments.TIMETABLE.getValue());
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                0, resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }
}
