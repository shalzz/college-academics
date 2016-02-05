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
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.shalzz.attendance.adapter.DayListAdapter;
import com.shalzz.attendance.fragment.DayFragment;

public class DayController {

    private DayFragment mView;
    public DayListAdapter mAdapter;

    public DayController(Context context, String day, DayFragment view) {
        mView = view;
        mAdapter = new DayListAdapter(context, day);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkAdapterIsEmpty();
            }
        });
        mView.mRecyclerView.setAdapter(mAdapter);
        checkAdapterIsEmpty();
    }

    private void checkAdapterIsEmpty () {
        if (mAdapter.getItemCount() == 0) {
            mView.mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mView.mEmptyView.setVisibility(View.GONE);
        }
    }
}
