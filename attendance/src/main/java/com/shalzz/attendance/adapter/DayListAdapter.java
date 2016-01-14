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

package com.shalzz.attendance.adapter;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.model.SubjectModel;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DayListAdapter extends RecyclerView.Adapter<DayListAdapter.ViewHolder>{

    private Context mContext;
    private String mDay;
    private SortedList<PeriodModel> mPeriods;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.tvSubjectName) TextView tvSubjectName;
        @InjectView(R.id.tvTime) TextView tvTime;
        @InjectView(R.id.tvTeacher) TextView tvTeacher;
        @InjectView(R.id.tvRoom) TextView tvRoom;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this,itemView);
        }
    }

    public DayListAdapter(Context context, String day){
        mContext = context;
        mDay = day;
        mPeriods = new SortedList<>(PeriodModel.class,
                new SortedListAdapterCallback<PeriodModel>(this) {
                    @Override
                    public int compare(PeriodModel o1, PeriodModel o2) {
                        return (int) (o1.getStartDate().getTime() - o2.getStartDate().getTime());
                    }

                    @SuppressWarnings("SimplifiableIfStatement")
                    @Override
                    public boolean areContentsTheSame(PeriodModel oldItem, PeriodModel newItem) {
                        if(oldItem.getId() != newItem.getId()) {
                            return false;
                        }
                        if(oldItem.getDay().equals(newItem.getDay())) {
                            return false;
                        }
                        if(!oldItem.getSubjectName().equals(newItem.getSubjectName())) {
                            return false;
                        }
                        if(oldItem.getTeacher().equals(newItem.getTeacher())) {
                            return false;
                        }
                        if(oldItem.getTime().equals(newItem.getTime())) {
                            return false;
                        }
                        if(oldItem.getRoom().equals(newItem.getRoom())) {
                            return false;
                        }
                        return oldItem.getBatch().equals(newItem.getBatch());
                    }

                    @Override
                    public boolean areItemsTheSame(PeriodModel item1, PeriodModel item2) {
                        return item1.getId() == item2.getId();
                    }
                });
        DatabaseHandler db = new DatabaseHandler(mContext);
        mPeriods.addAll(db.getAllPeriods(mDay));
    }

    public void addAll(List<PeriodModel> periods) {
        mPeriods.addAll(periods);
    }

    public void updatePeriods() {
        DatabaseHandler db = new DatabaseHandler(mContext);
        mPeriods.addAll(db.getAllPeriods(mDay));
    }

    public int getPeriodCount() {
        return mPeriods.size();
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

        PeriodModel period = mPeriods.get(position);
        holder.tvSubjectName.setText(period.getSubjectName());
        holder.tvRoom.setText(period.getRoom());
        holder.tvTeacher.setText(period.getTeacher());
        try {
            holder.tvTime.setText(period.getTimein12hr());
        } catch (ParseException e) {
            holder.tvTime.setText(period.getTime());
            e.printStackTrace();
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mPeriods.size();
    }

}
