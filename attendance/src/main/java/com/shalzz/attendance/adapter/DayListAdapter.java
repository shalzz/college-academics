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

package com.shalzz.attendance.adapter;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shalzz.attendance.R;
import com.shalzz.attendance.model.local.Day;
import com.shalzz.attendance.model.remote.Period;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DayListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private SortedList<Period> mPeriods;
    private List<Integer> subjectIDs;
    private SortedListAdapterCallback<Period> callback;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tvSubjectName) TextView tvSubjectName;
        @BindView(R.id.tvTime) TextView tvTime;
        @BindView(R.id.tvTeacher) TextView tvTeacher;
        @BindView(R.id.tvRoom) TextView tvRoom;
        @BindView(R.id.tvMarkedAbsent) TextView tvMarkedAbsent;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public DayListAdapter(){
        callback = new SortedListAdapterCallback<Period>(this) {
            @Override
            public int compare(Period o1, Period o2) {
                return (o1.getStartDate().compareTo(o2.getStartDate()));
            }

            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean areContentsTheSame(Period oldItem, Period newItem) {
                if(oldItem.id() != newItem.id()) {
                    return false;
                }
                if(!oldItem.week_day().equals(newItem.week_day())) {
                    return false;
                }
                if(!oldItem.name().equals(newItem.name())) {
                    return false;
                }
                if(!oldItem.teacher().equals(newItem.teacher())) {
                    return false;
                }
                if(!oldItem.start_time().equals(newItem.start_time())) {
                    return false;
                }
                if(!oldItem.end_time().equals(newItem.end_time())) {
                    return false;
                }
                if((oldItem.batch() != null && newItem.batch() != null)
                        && !oldItem.batch().equals(newItem.batch())) {
                    return false;
                }
                return oldItem.room().equals(newItem.room());
            }

            @Override
            public boolean areItemsTheSame(Period item1, Period item2) {
                return item1.id() == item2.id();
            }
        };

        mPeriods = new SortedList<>(Period.class, callback);
    }

    public void update(Day day) {
        List<Period> periods = day.getPeriods();
        subjectIDs = day.getSubjectIDs();
        mPeriods.beginBatchedUpdates();
        for (int i = 0; i < mPeriods.size(); i++) {
            Period existingObject = mPeriods.get(i);
            boolean found = false;
            for (Period newObject : periods) {
                if (callback.areItemsTheSame(existingObject, newObject)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mPeriods.remove(existingObject);
            }
        }
        mPeriods.addAll(periods);
        mPeriods.endBatchedUpdates();
    }

    public void clear() {
        mPeriods.clear();
    }


    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_day_item, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        DayListAdapter.ViewHolder holder = (DayListAdapter.ViewHolder) viewHolder;
        Period period = mPeriods.get(position);
        holder.tvSubjectName.setText(period.name());
        holder.tvRoom.setText(period.room());
        holder.tvTeacher.setText(period.teacher());
        holder.tvTime.setText(period.getTimein12hr());

        if(subjectIDs.contains(period.id()))
            holder.tvMarkedAbsent.setVisibility(View.VISIBLE);
        else {
            holder.tvMarkedAbsent.setVisibility(View.GONE);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mPeriods.size();
    }

}
