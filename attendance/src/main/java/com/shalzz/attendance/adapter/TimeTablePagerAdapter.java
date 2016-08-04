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
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.preference.PreferenceManager;

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.fragment.DayFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class TimeTablePagerAdapter extends FragmentStatePagerAdapter {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, Date> dates = new HashMap<>();
	private final HashMap<Date, Integer> positions = new HashMap<>();
    private Date mDate;
    private Context mContext;
    private int mCount;

	public TimeTablePagerAdapter(FragmentManager fm, Context context) {
		super(fm);
        mContext = context;
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getPeriodCount()<=0) {
            mCount = 0;
        } else {
            mCount = 31;
        }
        db.close();
	}

	@Override
	public DayFragment getItem(int position) {
        return DayFragment.newInstance(dates.get(position));
	}

    // fixme: doesn't work properly with date picker
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

    public int getPositionForDate(Date date) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean show = sharedPref.getBoolean(mContext.getString(R.string.pref_key_show_weekends), true);
        if(!show) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                calendar.add(Calendar.DATE, 1);
            }
            date = calendar.getTime();
        }

        return positions.get(date);
    }

    public void setDate(Date date) {
        mCount = 31;
        if(mDate != date) {
            mDate = date;
            updateDates();
        }
        notifyDataSetChanged();
    }

    public void updateDates() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean show = sharedPref.getBoolean(mContext.getString(R.string.pref_key_show_weekends), true);
        int day_offset = 0;
        Calendar calendar = Calendar.getInstance();
        for(int i =0; i < getCount() ; i++) {
            calendar.setTime(mDate);
            calendar.add(Calendar.DATE, -15+i);
            if(!show) {
                calendar.add(Calendar.DATE, day_offset);
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    calendar.add(Calendar.DATE, 1);
                    ++day_offset;
                }
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    calendar.add(Calendar.DATE, 1);
                    ++day_offset;
                }
            }
            Date date = calendar.getTime();
            dates.put(i, date);
            positions.put(date, i);
        }
    }
}
