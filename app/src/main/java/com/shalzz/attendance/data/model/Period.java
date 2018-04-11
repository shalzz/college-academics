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

package com.shalzz.attendance.data.model;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.google.gson.TypeAdapter;
import com.shalzz.attendance.model.PeriodModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;

import java.text.ParseException;
import java.util.Date;

import timber.log.Timber;

@AutoValue
public abstract class Period implements PeriodModel, Parcelable {

    public static final Factory<Period> FACTORY =
            new Factory<>(AutoValue_Period::new);

    public static final RowMapper<Period> MAPPER = FACTORY.selectByDateMapper();

    public static TypeAdapter<Period> typeAdapter(com.google.gson.Gson gson) {
        return new AutoValue_Period.GsonTypeAdapter(gson);
    }

    public static Builder builder() {
        return new AutoValue_Period.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(int id);
        public abstract Builder setName(String name);
        public abstract Builder setTeacher(String teacher);
        public abstract Builder setRoom(String room);
        public abstract Builder setBatchid(String batchid);
        public abstract Builder setBatch(String string);
        public abstract Builder setStart(String string);
        public abstract Builder setEnd(String string);
        public abstract Builder setDate(String string);
        public abstract Builder setAbsent(boolean absent);
        public abstract Period build();
    }

    private Date start_date;
    private String timeRange;

    public Date getStartDate() {
        if(start_date == null) {
            try {
                start_date = DateHelper.hr24Format.parse(start());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return start_date;
	}

    public String getTimein12hr() {
        if(timeRange == null) {
            String mStart, mEnd;
            try {
                mStart = DateHelper.to12HrFormat(start());
                mEnd = DateHelper.to12HrFormat(end());
            } catch (ParseException e) {
                e.printStackTrace();
                return start() + "-" + end();
            }

            // Remove leading zero's
            mStart = mStart.startsWith("0") ? mStart.substring(1) : mStart;
            mEnd = mEnd.startsWith("0") ? mEnd.substring(1) : mEnd;

            // If a range shares a common AM/PM, append only on the end of the range. (Material Guideline)
            int sl = mStart.length(), el = mEnd.length();
            if (mStart.substring(sl - 2).equals(mEnd.substring(el - 2)))
                timeRange = mStart.substring(0, sl - 3) + "-" + mEnd;
            else
                timeRange = mStart + "-" + mEnd;

        }
        return timeRange;
    }

}
