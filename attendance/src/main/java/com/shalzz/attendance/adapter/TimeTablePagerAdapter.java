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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.fragment.DayFragment;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Date;
import java.util.HashMap;

public class TimeTablePagerAdapter extends FragmentStatePagerAdapter {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, Date> dates = new HashMap<>();
	private final HashMap<Date, Integer> positions = new HashMap<>();
    private Date mDate;
    private Context mContext;
    private int mCount;

	public TimeTablePagerAdapter(FragmentManager fm, Context context, Date date) {
		super(fm);
        mContext = context;
        mDate = date;
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getTimetableCount()<=0) {
            mCount = 0;
        } else {
            mCount = 31;
        }
        db.close();
	}

	@Override
	public DayFragment getItem(int position) {
        Date date = DateHelper.addDays(mDate, -15+position);
        DayFragment fragment = DayFragment.newInstance(date);

        dates.put(position, date);
        positions.put(date,position);
		
		return fragment;
	}

    @Override
    public int getItemPosition(Object item) {
        DayFragment fragment = (DayFragment)item;
        Date date = fragment.getDate();
        int position = positions.get(date);

        if (position >= 0) {
            return position;
        } else {
            return POSITION_NONE;
        }
    }

    @Override
    public int getCount() {
        return mCount;
    }

    public Date getDate() {
        return mDate;
    }

    public Date getDateForPosition(int position) {
        return dates.get(position);
    }

    public void setDate(Date date) {
        if(mDate != date) {
            mDate = date;
            updateDates();
        }
        mCount = 31;
        notifyDataSetChanged();
    }

    public void updateDates() {
        for(int i =0; i<getCount(); i++) {
            Date date = DateHelper.addDays(mDate, -15+i);
            dates.put(i, date);
        }
    }
}
