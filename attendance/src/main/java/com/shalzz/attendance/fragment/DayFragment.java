/*
 * Copyright (c) 2014 Shaleen Jain <shaleen.jain95@gmail.com>
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.shalzz.attendance.model.Day;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MyVolley;
import com.squareup.leakcanary.RefWatcher;

import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DayFragment extends Fragment {

    @InjectView(R.id.time_table_recycler_view) RecyclerView mRecyclerView;
    @InjectView(R.id.empty_view) View mEmptyView;
    private Context mContext;
    private Date mDate;
    public static final String ARG_DATE = "date";
    private DayListAdapter mAdapter;
    private Day mDay;


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
        View mView = inflater.inflate(R.layout.timetable_view, container, false);
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
        super.onViewCreated(view, savedInstanceState);
        reloadDataSet();
        mAdapter = new DayListAdapter(mDay);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkAdapterIsEmpty();
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        checkAdapterIsEmpty();
    }

    public void reloadDataSet() {
        DatabaseHandler db = new DatabaseHandler(getActivity());
        String weekday = DateHelper.getTechnicalWeekday(mDate);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String pref_batch = sharedPref.getString(getString(R.string.pref_key_batch), "NULL");

        mDay = pref_batch.equals("NULL") ? db.getDay(weekday): db.getDay(weekday,pref_batch);

        if(mAdapter!=null)
            mAdapter.setDataSet(mDay);
    }

    private void checkAdapterIsEmpty () {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @NonNull
    public Date getDate() {
        return mDate;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
        RefWatcher refWatcher = MyVolley.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
