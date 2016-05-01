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
import com.shalzz.attendance.data.model.local.ImmutableDayModel;
import com.shalzz.attendance.data.model.remote.ImmutablePeriodModel;

import java.text.ParseException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DayListAdapter extends RecyclerView.Adapter<DayListAdapter.ViewHolder>{

    private SortedList<ImmutablePeriodModel> mPeriods;
    private List<Integer> subjectIDs;
    private SortedListAdapterCallback<ImmutablePeriodModel> callback;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tvSubjectName) TextView tvSubjectName;
        @BindView(R.id.tvTime) TextView tvTime;
        @BindView(R.id.tvTeacher) TextView tvTeacher;
        @BindView(R.id.tvRoom) TextView tvRoom;
        @BindView(R.id.tvMarkedAbsent) TextView tvMarkedAbsent;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public DayListAdapter(){
        callback = new SortedListAdapterCallback<ImmutablePeriodModel>(this) {
            @Override
            public int compare(ImmutablePeriodModel o1, ImmutablePeriodModel o2) {
                return (o1.getStartDate().compareTo(o2.getStartDate()));
            }

            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean areContentsTheSame(ImmutablePeriodModel oldItem, ImmutablePeriodModel newItem) {
                if(oldItem.getId() != newItem.getId()) {
                    return false;
                }
                if(oldItem.getDay().equals(newItem.getDay())) {
                    return false;
                }
                if(!oldItem.getName().equals(newItem.getName())) {
                    return false;
                }
                if(oldItem.getTeacher().equals(newItem.getTeacher())) {
                    return false;
                }
                if(oldItem.getStart().equals(newItem.getStart())) {
                    return false;
                }
                if(oldItem.getEnd().equals(newItem.getEnd())) {
                    return false;
                }
                if(oldItem.getRoom().equals(newItem.getRoom())) {
                    return false;
                }
                return oldItem.getBatch().equals(newItem.getBatch());
            }

            @Override
            public boolean areItemsTheSame(ImmutablePeriodModel item1, ImmutablePeriodModel item2) {
                return item1.getId() == item2.getId();
            }
        };

        mPeriods = new SortedList<>(ImmutablePeriodModel.class, callback);
    }

    public void update(ImmutableDayModel day) {
        List<ImmutablePeriodModel> periods = day.getPeriods();
        subjectIDs = day.getSubjectIDs();
        mPeriods.beginBatchedUpdates();
        for (int i = 0; i < mPeriods.size(); i++) {
            ImmutablePeriodModel existingObject = mPeriods.get(i);
            boolean found = false;
            for (ImmutablePeriodModel newObject : periods) {
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
    public DayListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_day_item, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        ImmutablePeriodModel period = mPeriods.get(position);
        holder.tvSubjectName.setText(period.getName());
        holder.tvRoom.setText(period.getRoom());
        holder.tvTeacher.setText(period.getTeacher());
        holder.tvTime.setText(period.getTimein12hr());

        if(subjectIDs.contains(period.getId()))
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
