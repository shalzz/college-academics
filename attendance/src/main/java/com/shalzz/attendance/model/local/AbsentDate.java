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

package com.shalzz.attendance.model.local;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.shalzz.attendance.wrapper.DateHelper;
import com.squareup.sqldelight.ColumnAdapter;

import java.util.Date;

@AutoValue
public abstract class AbsentDate implements AbsentDatesModel, Parcelable {
    private static final ColumnAdapter<Date> DATE_ADAPTER = new ColumnAdapter<Date>() {
        @Override
        public Date map(Cursor cursor, int columnIndex) {
            return DateHelper.parseDate(cursor.getString(columnIndex));
        }

        @Override
        public void marshal(ContentValues values, String key, Date value) {
            values.put(key, DateHelper.formatToTechnicalFormat(value));
        }
    };

    public static final Mapper<AbsentDate> MAPPER = new Mapper<>((Mapper.Creator<AbsentDate>)
            AbsentDate::create, DATE_ADAPTER);

    public static final class Marshal extends AbsentDatesMarshal<Marshal> {
        public Marshal() {
            super(DATE_ADAPTER);
        }
    }

    public static AbsentDate create(Integer subject_id, Date absent_date) {
        return new AutoValue_AbsentDate(subject_id, absent_date);
    }
}
