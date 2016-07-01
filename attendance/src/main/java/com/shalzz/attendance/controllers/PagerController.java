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
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;

import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.TimeTablePagerAdapter;
import com.shalzz.attendance.model.remote.Period;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.network.RetrofitException;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;

public class PagerController {

    private TimeTablePagerFragment mView;
    public TimeTablePagerAdapter mAdapter;
    private DatabaseHandler db;
    private Resources mResources;
    private String mTag = "Pager Controller";
    private Date mToday = new Date();
    private final DataAPI api;

    @Inject
    public PagerController(@Singleton Context context,
                           TimeTablePagerFragment view,
                           FragmentManager fm,
                           DataAPI api) {
        mResources = context.getResources();
        mView = view;
        db = new DatabaseHandler(context);
        mAdapter = new TimeTablePagerAdapter(fm, context);
        mView.mViewPager.setAdapter(mAdapter);
        this.api = api;
    }

    public void setDate(Date date) {
        mAdapter.setDate(date);
        mView.updateTitle(-1);
        scrollToDate(date);
    }

    public void setToday() {
        setDate(mToday);
    }

    public void scrollToDate(Date date) {
        int pos = mAdapter.getPositionForDate(date);
        mView.mViewPager.setCurrentItem(pos, true);
    }

    public Date getDateForPosition(int position) {
        return mAdapter.getDateForPosition(position);
    }

    public void updatePeriods() {
        Call<List<Period>> call = api.getTimetable();
        call.enqueue(new Callback<List<Period>>() {
            @Override
            public void onResponse(Call<List<Period>> call,
                                   retrofit2.Response<List<Period>> response) {
                done();
                toggleEmptyViewVisibility(false);

                List<Period> periods = response.body();

                long now = new Date().getTime();
                for (Period period : periods) {
                    db.addPeriod(period, now);
                }

                if (db.purgeOldPeriods() == 1) {
                    if(BuildConfig.DEBUG)
                        Log.d(mTag, "Purging Periods...");
                }

                // TODO: use an event bus or RxJava to update fragment contents

                setToday();
                mView.updateTitle(-1);
                db.close();

                // Update the drawer header
                ((MainActivity) mView.getActivity()).updateLastSync();
            }

            @Override
            public void onFailure(Call<List<Period>> call, Throwable t) {
                RetrofitException error = (RetrofitException) t;
                if (error.getKind() == RetrofitException.Kind.NETWORK) {
                    if(db.getPeriodCount() > 0) {
                        if(mView == null || mView.getActivity() == null)
                            return;
                        View view = mView.getActivity().findViewById(android.R.id.content);
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                .setAction("Retry", v -> updatePeriods())
                                .show();
                    } else {
                        Drawable emptyDrawable = new IconDrawable(mView.getContext(),
                                Iconify.IconValue.zmdi_wifi_off)
                                .colorRes(android.R.color.darker_gray);
                        mView.mEmptyImageView.setImageDrawable(emptyDrawable);
                        mView.mEmptyTitleTextView.setText(R.string.no_connection_title);
                        mView.mEmptyContentTextView.setText(R.string.no_connection_content);
                        mView.mEmptyButton.setOnClickListener( v -> updatePeriods());
                        mView.mEmptyButton.setVisibility(View.VISIBLE);

                        toggleEmptyViewVisibility(true);
                    }
                }
                else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE) {
                    Drawable emptyDrawable = new IconDrawable(mView.getContext(),
                            Iconify.IconValue.zmdi_cloud_off)
                            .colorRes(android.R.color.darker_gray);
                    mView.mEmptyImageView.setImageDrawable(emptyDrawable);
                    mView.mEmptyTitleTextView.setText(R.string.no_data_title);
                    mView.mEmptyContentTextView.setText(R.string.no_data_content);
                    mView.mEmptyButton.setVisibility(View.GONE);

                    toggleEmptyViewVisibility(true);

                    // Update the drawer header
                    ((MainActivity) mView.getActivity()).updateLastSync();
                }
                else if (error.getKind() == RetrofitException.Kind.HTTP) {
                    showError(error.getMessage());
                }
                else {
                    if(BuildConfig.DEBUG)
                        t.printStackTrace();

                    String msg = mResources.getString(R.string.unexpected_error);
                    showError(msg);
                }
                done();
            }
        });
    }

    private void toggleEmptyViewVisibility(boolean show) {
        if(mView == null || mView.mViewPager == null || mView.mEmptyView == null)
            return;
        if(show) {
            mView.mEmptyView.setVisibility(View.VISIBLE);
            mView.mViewPager.setVisibility(View.GONE);
        } else {
            mView.mEmptyView.setVisibility(View.GONE);
            mView.mViewPager.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String message) {
        if(mView == null || mView.getActivity() == null)
            return;
        View view = mView.getActivity().findViewById(android.R.id.content);
        if(view != null)
            Miscellaneous.showSnackBar(view, message);
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
