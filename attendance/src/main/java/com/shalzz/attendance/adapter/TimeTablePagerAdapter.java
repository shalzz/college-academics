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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.shalzz.attendance.fragment.DayFragment;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Date;
import java.util.HashMap;

public class TimeTablePagerAdapter extends FragmentStatePagerAdapter {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, DayFragment> activeFragments = new HashMap<Integer, DayFragment>();
	private final HashMap<Integer, Date> dates = new HashMap<>();
    private Date mDate;
	
	public TimeTablePagerAdapter(FragmentManager fm, Date date) {
		super(fm);
        mDate = date;
	}

	@Override
	public DayFragment getItem(int position) {
        Date date = DateHelper.addDays(mDate, -15+position);
        DayFragment fragment = DayFragment.newInstance(date);
		
		activeFragments.put(position, fragment);
        dates.put(position, date);
		
		return fragment;
	}

    @Override
    public int getCount() {
        return 31;
    }

    public Date getDate() {
        return mDate;
    }

    public Date getDateForPosition(int position) {
        return dates.get(position);
    }

    public void setDate(Date date) {
        mDate = date;
        updateDates();
        notifyDataSetChanged();
    }

    public void updateDates() {
        for(int i =0; i<getCount(); i++) {
            Date date = DateHelper.addDays(mDate, -15+i);
            dates.put(i, date);
        }
    }

    public void updateActiveFragments(boolean force) {
        for(int i = 0; i < activeFragments.size(); i++) {
            DayFragment fragment = activeFragments.get(i);
            if (fragment != null) {
                fragment.update(force);
            }
        }
    }

	@Override
	public void destroyItem(ViewGroup viewPager, int position, Object object) {
        activeFragments.remove(position);
		super.destroyItem(viewPager, position, object);
	}
}
