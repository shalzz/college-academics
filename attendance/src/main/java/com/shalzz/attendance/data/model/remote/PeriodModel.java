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

package com.shalzz.attendance.data.model.remote;

import com.shalzz.attendance.wrapper.DateHelper;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

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
@Value.Immutable
public abstract class PeriodModel {

	public abstract int getId();
    public abstract String getName();
    public abstract String getRoom();
    public abstract String getTeacher();
    public abstract String getStart();
    public abstract String getEnd();
    public abstract String getDay();
    public abstract String getBatch();

    @Gson.Ignore
    @Value.Derived
	public Date getStartDate() {
        Date d = null;
        try {
            d = DateHelper.hr24Format.parse(getStart());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
	}

    @Gson.Ignore
    @Value.Derived
    public String getTimein12hr() {
        String timeRange , mStart, mEnd;
        try {
            mStart = DateHelper.to12HrFormat(getStart());
            mEnd = DateHelper.to12HrFormat(getEnd());
        } catch(ParseException e) {
            e.printStackTrace();
            return getStart() + "-" + getEnd();
        }

        // Remove leading zero's
        mStart = mStart.startsWith("0") ? mStart.substring(1) : mStart;
        mEnd = mEnd.startsWith("0") ? mEnd.substring(1) : mEnd;

        // If a range shares a common AM/PM, append only on the end of the range. (Material Guideline)
        int sl = mStart.length(), el = mEnd.length();
        if(mStart.substring(sl-2).equals(mEnd.substring(el-2)))
            timeRange =mStart.substring(0,sl-3) + "-" + mEnd ;
        else
            timeRange =  mStart + "-" + mEnd ;

        return timeRange;
    }
}
