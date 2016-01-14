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

package com.shalzz.attendance.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.DividerItemDecoration;
import com.shalzz.attendance.R;
import com.shalzz.attendance.adapter.DayListAdapter;
import com.shalzz.attendance.controllers.DayController;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DayFragment extends Fragment {

    @InjectView(R.id.time_table_recycler_view)
    public RecyclerView mRecyclerView;
    @InjectView(R.id.empty_view)
    public View mEmptyView;

    private Context mContext;
    private Date mDate;
    private DayController mController;
    public static final String ARG_DATE = "date";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mDate = new Date();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(container==null)
            return null;
        View mView = inflater.inflate(R.layout.fragment_day, container, false);
        ButterKnife.inject(this,mView);

        mDate = (Date) getArguments().getSerializable(ARG_DATE);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST);
//        RecyclerView.ItemDecoration calendarItemDecoration =
//                new CalendarItemDecoration(mContext, mDate, CalendarItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);
//        mRecyclerView.addItemDecoration(calendarItemDecoration);
        // TODO: add calendar itemDecoration

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mController = new DayController(mContext, DateHelper.getShortWeekday(mDate), this);
    }

    public void update() {
        mController.mAdapter.updatePeriods();
    }

    public void clear() {
        mController.mAdapter.clear();
    }

    @NonNull
    public Date getDate() {
        return mDate;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
