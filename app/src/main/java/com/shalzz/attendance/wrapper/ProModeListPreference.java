/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.wrapper;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

/**
 * @author shalzz
 */
public class ProModeListPreference extends ListPreference {

    private Context mContext;
    private OnProModeListPreferenceClickListener mCallback;

    public ProModeListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    public ProModeListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ProModeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProModeListPreference(Context context) {
        super(context);
    }

    public void showDialog() {
        super.onClick();
    }

    public void setProModeListPreferenceClickListener(OnProModeListPreferenceClickListener listener) {
        mCallback = listener;
    }

    @Override
    protected void onClick() {
        if (mCallback != null)
            mCallback.onPreferenceClick(this);
    }

    public interface OnProModeListPreferenceClickListener {
        boolean onPreferenceClick(ProModeListPreference preference);
    }
}
