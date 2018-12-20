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

package com.shalzz.attendance.data.local

import com.shalzz.attendance.wrapper.DateHelper

import java.util.ArrayList
import java.util.Date
import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromString(databaseValue: String): Date {
        return DateHelper.parseDate(databaseValue)
    }

    @TypeConverter
    fun dateToString(value: Date): String {
        return DateHelper.toTechnicalFormat(value)
    }

    @TypeConverter
    fun fromDateListString(databaseValue: String): List<Date> {
        val dates = ArrayList<Date>()
        for (date in databaseValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            dates.add(DateHelper.parseDate(date))
        }
        return dates
    }

    @TypeConverter
    fun dateListToString(value: List<Date>): String {
        val SEPARATOR = ","
        val csvBuilder = StringBuilder()
        for (date in value) {
            csvBuilder.append(DateHelper.toTechnicalFormat(date))
            csvBuilder.append(SEPARATOR)
        }

        var csv = csvBuilder.toString()
        //Remove last comma
        return if (csv.isEmpty()) csv else csv.substring(0, csv.length - SEPARATOR.length)
    }
}
