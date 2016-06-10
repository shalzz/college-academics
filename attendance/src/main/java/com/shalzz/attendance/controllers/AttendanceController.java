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
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.ExpandableListAdapter;
import com.shalzz.attendance.data.model.remote.Subject;
import com.shalzz.attendance.data.network.DataAPI;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.loader.SubjectAsyncTaskLoader;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceController implements LoaderManager.LoaderCallbacks<List<Subject>> {

    public static final String SUBJECT_FILTER = "subject_filter_text";

    private AttendanceListFragment mView;
    private ExpandableListAdapter mAdapter;
    private DatabaseHandler db;
    private Context mContext;
    private Resources mResources;
    private String mTag = "Attendance Controller";
    private View mFooter;
    private final DataAPI api;

    @Inject
    public AttendanceController(Context context,
                                AttendanceListFragment view,
                                DataAPI api) {
        mContext = context;
        mResources = context.getResources();
        mView = view;
        db = new DatabaseHandler(mContext);
        this.api = api;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        int expandLimit = Integer.parseInt(sharedPref.getString(
                mContext.getString(R.string.pref_key_sub_limit), "3"));

        mAdapter = new ExpandableListAdapter(mContext, mView);
        mAdapter.setLimit(expandLimit);
        LayoutInflater inflater = mView.getActivity().getLayoutInflater();
        mFooter = inflater.inflate(R.layout.list_footer, mView.mRecyclerView, false);
        mFooter.setVisibility(View.INVISIBLE);
        mAdapter.addFooter(mFooter);
        mView.mRecyclerView.setAdapter(mAdapter);
    }

    public void updateSubjects() {
        Call<List<Subject>> call = api.getAttendance();
        call.enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call,
                                   Response<List<Subject>> response) {
                done();
                if(response.isSuccessful()) {
                    List<Subject> subjects = response.body();
                    try {
                        if(subjects.size() > 0) {
                            long now = new Date().getTime();
                            for (Subject subject : subjects) {
                                db.addSubject(subject, now);
                            }

                            if (db.purgeOldSubjects() == 1) {
                                if(BuildConfig.DEBUG)
                                    Log.i(mTag, "Purging Subjects...");
                                mAdapter.clear();
                            }

                            db.close();
                            mAdapter.addAll(subjects);
                            mView.showcaseView();
                        } else {
                            String msg = mResources.getString(R.string.unavailable_data_error_msg);
                            Miscellaneous.showSnackBar(mView.mSwipeRefreshLayout, msg);
                        }
                        // Update the drawer header
                        ((MainActivity) mView.getActivity()).updateLastSync();
                    }
                    catch (Exception e) {
                        String msg = mResources.getString(R.string.unexpected_error);
                        Miscellaneous.showSnackBar(mView.mSwipeRefreshLayout, msg);
                        if(BuildConfig.DEBUG)
                            e.printStackTrace();
                    }
                } else {
                    Miscellaneous.showSnackBar(mView.mSwipeRefreshLayout, response.raw().message());
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                done();
                Miscellaneous.showSnackBar(mView.mSwipeRefreshLayout, t.getLocalizedMessage());
                if(BuildConfig.DEBUG)
                    t.printStackTrace();
            }
        });
    }

    public void done() {
        if(mView.mProgress != null) {
            mView.mProgress.setVisibility(View.GONE);
        }
        if(mView.mSwipeRefreshLayout != null) {
            mView.mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public Loader<List<Subject>> onCreateLoader(int id, Bundle args) {
        String filter = null;
        if(args != null)
            filter = args.getString(SUBJECT_FILTER);
        return new SubjectAsyncTaskLoader(mContext, filter);
    }

    @Override
    public void onLoadFinished(Loader<List<Subject>> loader, List<Subject> data) {
        String filter = ((SubjectAsyncTaskLoader) loader).mCurFilter;
        if(data.size() == 0 && filter == null) {
            updateSubjects();
        } else {
            done();
            mAdapter.addAll(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Subject>> loader) {
        // Loader reset, throw away our data,
        // unregister any listeners, etc.
        mAdapter.clear();
        // Of course, unless you use destroyLoader(),
        // this is called when everything is already dying
        // so a completely empty onLoaderReset() is
        // totally acceptable
    }
}
