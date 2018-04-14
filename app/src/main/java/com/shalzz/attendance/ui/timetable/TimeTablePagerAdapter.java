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

package com.shalzz.attendance.ui.timetable;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.preference.PreferenceManager;
import android.util.SparseArray;

import com.shalzz.attendance.R;
import com.shalzz.attendance.ui.day.DayFragment;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.annotations.NonNull;
import timber.log.Timber;

public class TimeTablePagerAdapter extends FragmentStatePagerAdapter {

    private final int COUNT = 31;

	private final SparseArray<Date> dates = new SparseArray<>();
    private Date mToday;
    private Date mDate;
    private boolean mShowWeekends;
    private Callback mCallback;

	TimeTablePagerAdapter(FragmentManager fm, Context context, Callback callback) {
		super(fm);
        mCallback = callback;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mShowWeekends = sharedPref.getBoolean(context.getString(R.string
                .pref_key_show_weekends), true);

        mToday = new Date();
        setDate(mToday);
	}

	@Override
	public DayFragment getItem(int position) {
        return DayFragment.newInstance(dates.get(position));
	}

    @Override
    public int getCount() {
        return COUNT;
    }

    @Override
    public int getItemPosition(Object item) {
	    // TODO: fix performance of fragment and array creation and destruction
//        DayFragment fragment = (DayFragment)item;
//        Date date = fragment.getDate();
//        int position = positions.get(date);
//
//        if (position >= 0) {
//            return position;
//        } else {
//            return POSITION_NONE;
//        }
        return POSITION_NONE;
    }

    public Date getDateForPosition(int position) {
        return dates.get(position);
    }

    public void scrollToDate(Date date) {
        if(!mShowWeekends) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            while(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
                calendar.add(Calendar.DATE, 1);
            }
            date = calendar.getTime();
        }
        Timber.d("Date: %s", date);
        mCallback.scrollToPosition(indexOfValue(dates, date));
    }

    public void scrollToToday() {
        scrollToDate(mToday);
    }

    public void setDate(@NonNull Date date) {
        if(mDate == null || !DateHelper.toTechnicalFormat(mDate)
                .equals(DateHelper.toTechnicalFormat(date))) {
            mDate = date;
            updateDates();
        }
    }

    private void updateDates() {
        Timber.d("Updating dates");
        int day_offset = 0;
        Calendar calendar = Calendar.getInstance();
        for(int i =0; i < getCount() ; i++) {
            calendar.setTime(mDate);
            calendar.add(Calendar.DATE, -15+i);
            if(!mShowWeekends) {
                calendar.add(Calendar.DATE, day_offset);
                while(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ) {
                    calendar.add(Calendar.DATE, 1);
                    ++day_offset;
                }
            }
            Date date = calendar.getTime();
            dates.put(i, date);
        }
        notifyDataSetChanged();
    }

    private int indexOfValue(SparseArray<Date> array, Date value) {
        for (int i = 0; i < array.size(); i++) {
            if (DateHelper.toTechnicalFormat(array.valueAt(i))
                    .equals(DateHelper.toTechnicalFormat(value)))
                return i;
        }
        return -1;
    }

    interface Callback {
	    void scrollToPosition(int position);
    }
}
