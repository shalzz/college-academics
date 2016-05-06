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

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.shalzz.attendance.wrapper.DateHelper;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Field names need to be the same
 *  as that of the fields in the
 *  JSON object sent by the REST API,
 *  for {@link com.google.gson.Gson} to be able to deserialize it
 *  properly and automatically.
 *
 *  Typical `attendance` JSON object will be of the format:
 *  {
 *      "absent_dates":["","",...],
 *      "id": ##,
 *      "name":"",
 *      "held": ##,
 *      "attended": ##}
 *  }
 *
 *  which is exposed by the api endpoint /api/v1/me/attendance
 *  by the express.js server (upes-api) as of this writing.
 */
@Value.Immutable
@Value.Style(allParameters = true)
public abstract class Subject implements SubjectModel{
    public static final Mapper<ImmutableSubject> MAPPER =
            new SubjectModel.Mapper<>(new Mapper.Creator<ImmutableSubject>() {
                @Override
                public ImmutableSubject create(int id, String name, Float attended, Float held,
                                      Long last_updated) {
                    return ImmutableSubject.of(id,name,attended,held);
                }
            });

    public static final class Marshal extends SubjectMarshal<Marshal> { }

    @Nullable
    @Override
    @Gson.Ignore
    @Value.Parameter(false)
    public abstract Long last_updated();

    @SerializedName("absent_dates")
    @Value.Parameter(false)
    public abstract List<Date> getAbsentDates();

    @Gson.Ignore
    @Value.Derived
	public String getAbsentDatesAsString() {
        DateFormat dayFormat = new SimpleDateFormat("d", Locale.US);
        DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.US);
        List<Date> dates = new ArrayList<>();
        dates.addAll(getAbsentDates());
        if(dates.size() == 0)
            return "";

		String datesStr = "";
        String prevMonth = "";
        Collections.sort(dates);
        for (Date date: dates) {
            int day = Integer.parseInt(dayFormat.format(date));
            String month = monthFormat.format(date);
            if(prevMonth.length() == 0) {
                datesStr += month + ": ";
                prevMonth = month;
            }
            else if(!prevMonth.equals(month)) {
                datesStr += "\n" + month + ": ";
                prevMonth = month;
            }
            datesStr += day + DateHelper.getDayOfMonthSuffix(day) + ", ";
        }
        return datesStr.substring(0,datesStr.length()-2);
	}

    @Gson.Ignore
    @Value.Derived
	public Float getPercentage() {
        if(held() > 0f)
            return attended() / held() * 100;
        return 0.0f;
	}
}
