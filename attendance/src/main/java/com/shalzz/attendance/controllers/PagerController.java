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

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyVolley;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import java.util.ArrayList;
import java.util.Date;

public class PagerController {

    private TimeTablePagerFragment mView;
    private DatabaseHandler db;
    private Context mContext;
    private Resources mResources;
    private String mTag = "Pager Controller";

    public PagerController(Context context, TimeTablePagerFragment view) {
        mContext = context;
        mResources = MyVolley.getMyResources();
        mView = view;
        db = new DatabaseHandler(mContext);
    }

    public void updatePeriods() {
        DataAPI.getTimeTable(successListener(), errorListener());
    }

    public Response.Listener<ArrayList<PeriodModel>> successListener() {
        return new Response.Listener<ArrayList<PeriodModel>>() {
            @Override
            public void onResponse(ArrayList<PeriodModel> response) {
                try {

                    done();
                    if(response.size() > 0) {
                        long now = new Date().getTime();
                        for (PeriodModel period : response) {
                            db.addOrUpdatePeriod(period, now);
                        }

                        if (db.purgeOldPeriods() == 1) {
                            if(BuildConfig.DEBUG)
                                Log.d(mTag, "Purging Periods...");
                            mView.clearFragmentsData();
                        }

                        mView.updateFragmentsData();
                        mView.scrollToToday();
                        db.close();
                    } else {
                        String msg = mResources.getString(R.string.unavailable_timetable_error_msg);
                        Miscellaneous.showSnackBar(mContext,msg);
                    }
                    // Update the drawer header
                    ((MainActivity) mView.getActivity()).updateLastSync();
                }
                catch (Exception e) {
                    String msg = mResources.getString(R.string.unexpected_error);
                    Miscellaneous.showSnackBar(mContext, msg);
                    Bugsnag.notify(e);
                    if(BuildConfig.DEBUG)
                        e.printStackTrace();
                }
            }
        };
    }

    public Response.ErrorListener errorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                done();
                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                Miscellaneous.showSnackBar(mContext, msg);
                if(BuildConfig.DEBUG)
                    error.printStackTrace();
            }
        };
    }

    public void done() {
        if(mView.mProgress != null) {
            mView.mProgress.setVisibility(View.GONE);
        }
        if(mView.mSwipeRefreshLayout != null) {
            mView.mSwipeRefreshLayout.setRefreshing(false);
        }
        if(mView.mViewPager != null) {
            mView.mViewPager.setVisibility(View.VISIBLE);
        }
    }
}
