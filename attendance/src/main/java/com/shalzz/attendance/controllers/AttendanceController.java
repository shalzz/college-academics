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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.ExpandableListAdapter;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyVolley;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import java.util.ArrayList;
import java.util.Date;

public class AttendanceController {

    private AttendanceListFragment mView;
    private ExpandableListAdapter mAdapter;
    private DatabaseHandler db;
    private Context mContext;
    private Resources mResources;
    private String mTag = "Attendance Controller";
    private View mFooter;

    public AttendanceController(Context context, AttendanceListFragment view) {
        mContext = context;
        mResources = MyVolley.getMyResources();
        mView = view;
        db = new DatabaseHandler(mContext);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        int expandLimit = Integer.parseInt(sharedPref.getString(
                mContext.getString(R.string.pref_key_sub_limit), "3"));

        mAdapter = new ExpandableListAdapter(mContext, mView);
        mAdapter.setLimit(expandLimit);
        LayoutInflater inflater = mView.getActivity().getLayoutInflater();
        mFooter = inflater.inflate(R.layout.list_footer, mView.mRecyclerView, false);
        mAdapter.addFooter(mFooter);
        mView.mRecyclerView.setAdapter(mAdapter);
    }

    public void getSubjects() {
        mAdapter.addAll(db.getAllSubjects());
        db.close();
    }

    public void getSubjectsLike(String arg0) {
        // remove the existing subjects so only
        // the subjects matching the search criteria are shown.
        mAdapter.clear();
        mAdapter.addAll(db.getAllSubjectsLike(arg0));
        db.close();
    }

    public boolean hasSubjects() {
        return mAdapter.getSubjectCount() > 0;
    }

    public void updateSubjects() {
        if(!hasSubjects()) {
            mView.mProgress.setVisibility(View.VISIBLE);
            mFooter.setVisibility(View.GONE);
        }
        DataAPI.getAttendance(successListener(), errorListener());
    }

    public Response.Listener<ArrayList<SubjectModel>> successListener() {
        return new Response.Listener<ArrayList<SubjectModel>>() {
            @Override
            public void onResponse(ArrayList<SubjectModel> response) {
                try {

                    done();
                    if(response.size() > 0) {
                        long now = new Date().getTime();
                        for (SubjectModel subject : response) {
                            db.addOrUpdateSubject(subject, now);
                        }

                        if (db.purgeOldSubjects() == 1) {
                            if(BuildConfig.DEBUG)
                                Log.i(mTag, "Purging Subjects...");
                            mAdapter.clear();
                        }

                        mAdapter.addAll(response);
                        mFooter.setVisibility(View.VISIBLE);
                        mAdapter.updateFooter();
                    } else {
                        String msg = mResources.getString(R.string.unavailable_data_error_msg);
                        Miscellaneous.showSnackBar(mContext,msg);
                    }
                    // Update the drawer header
                    ((MainActivity) mView.getActivity()).updateLastSync();
                }
                catch (Exception e) {
                    String msg = mResources.getString(R.string.unexpected_error);
                    Miscellaneous.showSnackBar(mContext,msg);
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
    }
}
