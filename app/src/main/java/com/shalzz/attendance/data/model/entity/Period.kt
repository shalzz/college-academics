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

package com.shalzz.attendance.data.model.entity

import androidx.room.Entity
import com.shalzz.attendance.wrapper.DateHelper
import com.squareup.moshi.JsonClass
import java.text.ParseException
import java.util.*

@JsonClass(generateAdapter = true)
@Entity(primaryKeys = ["id", "date"])
data class Period(
        val id: Int,
        val name: String,
        val teacher: String,
        val room: String,
        val batchid: String,
        val batch: String?,
        val start: String,
        val end: String,
        val absent: Boolean,
        val date: String
) {

    val startDate: Date?
        get() {
            return DateHelper.hr24Format.parse(start)
        }

    // Remove leading zero's
    // If a range shares a common AM/PM, append only on the end of the range. (Material Guideline)
    val timein12hr: String
        get() {
            var mStart: String
            var mEnd: String
            try {
                mStart = DateHelper.to12HrFormat(start)
                mEnd = DateHelper.to12HrFormat(end)
            } catch (e: ParseException) {
                e.printStackTrace()
                return "$start-$end"
            }

            mStart = if (mStart.startsWith("0")) mStart.substring(1) else mStart
            mEnd = if (mEnd.startsWith("0")) mEnd.substring(1) else mEnd
            val sl = mStart.length
            val el = mEnd.length
            return if (mStart.substring(sl - 2) == mEnd.substring(el - 2))
                mStart.substring(0, sl - 3) + "-" + mEnd
            else
                "$mStart-$mEnd"
        }
}
