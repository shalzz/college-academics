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
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import java.util.ArrayList;
import java.util.Date;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	// Global variables
	private String myTag = "Sync Adapter";
	private Context mContext;
	
	/**
	 * Set up the sync adapter
	 */
	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		mContext = context;
	}

	/**
	 * Set up the sync adapter. This form of the
	 * constructor maintains compatibility with Android 3.0
	 * and later platform versions
	 */
	@SuppressLint("NewApi")
	public SyncAdapter(
			Context context,
			boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
        Bugsnag.leaveBreadcrumb("Running sync adapter");

		DataAPI.getAttendance(attendanceSuccessListener(), myErrorListener());
		DataAPI.getTimeTable(timeTableSuccessListener(), myErrorListener());
	}

	private Response.Listener<ArrayList<SubjectModel>> attendanceSuccessListener() {
		return new Response.Listener<ArrayList<SubjectModel>>() {
			@Override
			public void onResponse(ArrayList<SubjectModel> response) {
                try {
                    Bugsnag.leaveBreadcrumb("Sync: got attendance");
					DatabaseHandler db = new DatabaseHandler(mContext);
                    long now = new Date().getTime();
                    for (SubjectModel subject : response) {
                        db.addOrUpdateSubject(subject, now);
                    }
                    db.purgeOldSubjects();
					db.close();
                }
                catch(Exception e) {
                    Bugsnag.notify(e);
                    e.printStackTrace();
                }
			}
		};
	}
	
	private Response.Listener<ArrayList<PeriodModel>> timeTableSuccessListener() {
		return new Response.Listener<ArrayList<PeriodModel>>() {
			@Override
			public void onResponse(ArrayList<PeriodModel> response) {
                try {
                    Bugsnag.leaveBreadcrumb("Sync: got timetable");
					DatabaseHandler db = new DatabaseHandler(mContext);
                    long now = new Date().getTime();
					for(PeriodModel period : response) {
						db.addOrUpdatePeriod(period, now);
					}
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences
                            (mContext);
                    boolean notify = sharedPref.getBoolean(mContext.getString(
                            R.string.pref_key_notify_timetable_changed),
                            true);

                    notify = db.purgeOldPeriods() == 1 && notify;

                    // Show a notification since our timetable has changed.
                    if (notify) {
                        Bugsnag.leaveBreadcrumb("Notifying user");
                        NotificationCompat.Builder mBuilder =
                                (NotificationCompat.Builder) new NotificationCompat.Builder(mContext)
                                        .setSmallIcon(R.drawable.human)
                                        .setLargeIcon(BitmapFactory.decodeResource(
                                                        mContext.getResources(),
                                                        R.mipmap.ic_launcher))
                                        .setAutoCancel(true)
                                        .setPriority(Notification.PRIORITY_LOW)
                                        .setCategory(Notification.CATEGORY_RECOMMENDATION)
                                        .setContentTitle(mContext.getString(
                                                R.string.notify_timetable_changed_title))
                                        .setContentText(mContext.getString(
                                                R.string.notify_timetable_changed_text));

                        Intent resultIntent = new Intent(mContext, MainActivity.class);
                        resultIntent.putExtra(MainActivity.LAUNCH_FRAGMENT_EXTRA, MainActivity
                                .Fragments.TIMETABLE.getValue());
                        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext,
                                0, resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                        mBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager mNotificationManager =
                                (NotificationManager) mContext.getSystemService(
                                        Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(0, mBuilder.build());
                    }
					db.close();
                }
                catch(Exception e) {
                    Bugsnag.notify(e);
                    e.printStackTrace();
                }
			}
		};
	}

	private Response.ErrorListener myErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				String msg = MyVolleyErrorHelper.getMessage(error, mContext);
				Log.e(myTag, msg);
			}
		};
	}
}
