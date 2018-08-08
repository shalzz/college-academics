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
import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.shalzz.attendance.model.AbsentDatesModel;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;

import java.util.Date;

@AutoValue
public abstract class AbsentDate implements AbsentDatesModel, Parcelable {
    private static final ColumnAdapter<Date, String> DATE_ADAPTER =
            new ColumnAdapter<Date, String>() {

                @NonNull
                @Override
                public Date decode(String databaseValue) {
                    return DateHelper.parseDate(databaseValue);
                }

                @Override
                public String encode(@NonNull Date value) {
                    return DateHelper.toTechnicalFormat(value);
                }

            };

    public static final Factory<AbsentDate> FACTORY = new Factory<>(
            AutoValue_AbsentDate::new, DATE_ADAPTER);

    public static final RowMapper<AbsentDate> MAPPER = FACTORY.selectByIdMapper();

    public static AbsentDate create(Integer subject_id, Date absent_date) {
        return new AutoValue_AbsentDate(subject_id, absent_date);
    }
}
