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
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.shalzz.attendance.model.remote.PeriodModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqldelight.RowMapper;

import org.immutables.gson.Gson;

import java.text.ParseException;
import java.util.Date;

/** Field names need to be the same
 *  as that of the fields in the
 *  JSON object sent by the REST API,
 *  for {@link com.google.gson.Gson} to be able to deserialize it
 *  properly and automatically.
 *
 *  Typical `period` JSON object will be of the format:
 *  {
 *      "id": ##,
 *      "day": "",
 *      "name": "",
 *      "start": "",
 *      "end": "",
 *      "teacher": "",
 *      "room": "",
 *      "batch": ""
 *  }
 *
 *  which is exposed by the api endpoint /api/v1/me/timetable
 *  by the express.js server (upes-api) as of this writing.
 */
@AutoValue
public abstract class Period implements PeriodModel, Parcelable {
    public static final Factory<Period> FACTORY =
            new Factory<>(AutoValue_Period::new);

    public static final RowMapper<Period> MAPPER = FACTORY.select_by_week_dayMapper();

    public static TypeAdapter<Period> typeAdapter(com.google.gson.Gson gson) {
        return new AutoValue_Period.GsonTypeAdapter(gson);
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
