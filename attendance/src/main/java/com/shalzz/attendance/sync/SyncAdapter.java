/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
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
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
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

		DataAPI.getAttendance(attendanceSuccessListener(), myErrorListener());
		DataAPI.getTimeTable(timeTableSuccessListener(), myErrorListener());
	}   
	
	private Response.Listener<ArrayList<SubjectModel>> attendanceSuccessListener() {
		return new Response.Listener<ArrayList<SubjectModel>>() {
			@Override
			public void onResponse(ArrayList<SubjectModel> response) {
                try {
					DatabaseHandler db = new DatabaseHandler(mContext);
                    long now = new Date().getTime();
                    for (SubjectModel subject : response) {
                        db.addOrUpdateSubject(subject, now);
                    }
                    db.purgeSubjects();
					db.close();
                }
                catch(Exception e) {
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
					DatabaseHandler db = new DatabaseHandler(mContext);
                    long now = new Date().getTime();
					for(PeriodModel period : response) {
						db.addOrUpdatePeriod(period, now);
					}
                    db.purgePeriods();
					db.close();
                }
                catch(Exception e) {
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