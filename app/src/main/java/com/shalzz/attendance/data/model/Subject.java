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
import com.shalzz.attendance.model.SubjectModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class Subject implements SubjectModel, Parcelable {

    private static final ColumnAdapter<List<Date>, String> DATE_ADAPTER =
            new ColumnAdapter<List<Date>, String>() {

                @NonNull
                @Override
                public List<Date> decode(String databaseValue) {
                    List<Date> dates = new ArrayList<>();
                    for(String date : databaseValue.split(",")) {
                        dates.add(DateHelper.parseDate(date));
                    }
                    return dates;
                }

                @Override
                public String encode(@NonNull List<Date> value) {
                    String SEPARATOR = ",";
                    StringBuilder csvBuilder = new StringBuilder();
                    for(Date date : value){
                        csvBuilder.append(DateHelper.toTechnicalFormat(date));
                        csvBuilder.append(SEPARATOR);
                    }

                    String csv = csvBuilder.toString();
                    //Remove last comma
                    csv = csv.substring(0, csv.length() - SEPARATOR.length());

                    return csv;
                }

            };

    public static final Factory<Subject> FACTORY = new Factory<>(AutoValue_Subject::new, DATE_ADAPTER);

    public static final RowMapper<Subject> MAPPER = FACTORY.selectLikeNameMapper();

    public static TypeAdapter<Subject> typeAdapter(com.google.gson.Gson gson) {
        return new AutoValue_Subject.GsonTypeAdapter(gson);
    }

    public static Builder builder() {
        return new AutoValue_Subject.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setId(int id);
        public abstract Builder setName(String name);
        public abstract Builder setAttended(float attended);
        public abstract Builder setHeld(float held);
        public abstract Builder setAbsent_dates(List<Date> dateList);
        public abstract Subject build();
    }

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

}
