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

package com.shalzz.attendance.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shalzz.attendance.wrapper.DateHelper
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class Subject (
        @PrimaryKey val id: Int,
        val name: String,
        val attended: Float,
        val held: Float,
        val absent_dates: List<Date>?
) {
    val absentDatesAsString: String
        get() {
            if (absent_dates == null) {
                return ""
            }
            val dayFormat = SimpleDateFormat("d", Locale.US)
            val monthFormat = SimpleDateFormat("MMM", Locale.US)
            val dates = ArrayList<Date>()
            dates.addAll(absent_dates)
            if (dates.size == 0)
                return ""

            val datesStr = StringBuilder()
            var prevMonth = ""
            dates.sort()
            for (date in dates) {
                val day = Integer.parseInt(dayFormat.format(date))
                val month = monthFormat.format(date)
                if (prevMonth.isEmpty()) {
                    datesStr.append(month).append(": ")
                    prevMonth = month
                } else if (prevMonth != month) {
                    datesStr.append("\n").append(month).append(": ")
                    prevMonth = month
                }
                datesStr.append(day).append(DateHelper.getDayOfMonthSuffix(day)).append(", ")
            }
            return datesStr.substring(0, datesStr.length - 2)
        }

    fun getPercentage(): Float {
        return if (held > 0f) attended / held * 100 else 0.0f
    }

}
