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

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.TypeAdapter;
import com.shalzz.attendance.wrapper.DateHelper;

import org.immutables.gson.Gson;

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
@AutoValue
public abstract class Subject implements SubjectModel, Parcelable {
    public static final Mapper<Subject> MAPPER = new SubjectModel.Mapper<>(
            (id, name, attended, held, last_updated) -> Subject.builder()
                    .id(id)
                    .name(name)
                    .attended(attended)
                    .held(held)
                    .last_updated(last_updated)
                    .build()
    );

    public static final class Marshal extends SubjectMarshal<Marshal> { }

    @Nullable
    @Override
    @Gson.Ignore
    public abstract Long last_updated();

    @Nullable
    public abstract List<Date> absent_dates();

    private String absentDates;
    private Float percentage;

	public final String getAbsentDatesAsString() {
        if(absentDates == null) {
            if(absent_dates() == null) {
                absentDates = "";
                return absentDates;
            }
            DateFormat dayFormat = new SimpleDateFormat("d", Locale.US);
            DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.US);
            List<Date> dates = new ArrayList<>();
            dates.addAll(absent_dates());
            if (dates.size() == 0)
                return "";

            String datesStr = "";
            String prevMonth = "";
            Collections.sort(dates);
            for (Date date : dates) {
                int day = Integer.parseInt(dayFormat.format(date));
                String month = monthFormat.format(date);
                if (prevMonth.length() == 0) {
                    datesStr += month + ": ";
                    prevMonth = month;
                } else if (!prevMonth.equals(month)) {
                    datesStr += "\n" + month + ": ";
                    prevMonth = month;
                }
                datesStr += day + DateHelper.getDayOfMonthSuffix(day) + ", ";
            }
            absentDates = datesStr.substring(0,datesStr.length()-2);
        }
        return absentDates;
	}

	public final Float getPercentage() {
        if(percentage == null) {
            percentage = held() > 0f ? attended() / held() * 100 : 0.0f;
        }
        return percentage;
	}

    public static Builder builder() {
        return new AutoValue_Subject.Builder();
    }

    public Builder toBuilder() {
        return new AutoValue_Subject.Builder(this);
    }

    public final Subject withAbsentDates(List<Date> dates) {
        return toBuilder().absent_dates(dates).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(int value);
        public abstract Builder name(String value);
        public abstract Builder attended(Float value);
        public abstract Builder held(Float value);
        public abstract Builder absent_dates(List<Date> value);
        public abstract Builder last_updated(Long value);
        public abstract Subject build();
    }

    public static TypeAdapter<Subject> typeAdapter(com.google.gson.Gson gson) {
        return new AutoValue_Subject.GsonTypeAdapter(gson);
    }
}
