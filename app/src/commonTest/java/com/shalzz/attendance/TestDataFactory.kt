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

package com.shalzz.attendance

import com.shalzz.attendance.data.model.entity.*
import com.shalzz.attendance.wrapper.DateHelper
import java.util.*

/**
 * Factory class that makes instances of data models with random field values.
 * The aim of this class is to help setting up test fixtures.
 */
object TestDataFactory {

    private fun randomUuid(): String {
        return UUID.randomUUID().toString()
    }

    private fun makeInt(string: String): Int {
        val sb = StringBuilder()
        for (c in string.toCharArray()) {
            sb.append(c.toInt())
        }
        return Integer.valueOf(sb.toString())
    }

    fun makeUser(uniqueSuffix: String): User {
        return User(
                phone = "Phone-$uniqueSuffix",
                roll_number = randomUuid(),
                name = "Name-$uniqueSuffix",
                course = "Course-$uniqueSuffix",
                email = "email$uniqueSuffix@gmail.com")
    }

     fun makeSubject(uniqueSuffix: String): Subject {
        return Subject(
                id = makeInt(uniqueSuffix),
                name = "Name-$uniqueSuffix",
                attended = 1f,
                held = 2f,
                absent_dates = ArrayList())
    }

    fun makePeriod(uniqueSuffix: String, date: Date): Period {
        return Period(
                id = makeInt(uniqueSuffix),
                name = "Name-$uniqueSuffix",
                teacher = "Teacher-$uniqueSuffix",
                room = "Room-$uniqueSuffix",
                batchid = "Batchid-$uniqueSuffix",
                batch = "Batch-$uniqueSuffix",
                start = "Start-$uniqueSuffix",
                end = "End-$uniqueSuffix",
                absent = uniqueSuffix.length % 2 == 0,
                date = DateHelper.toTechnicalFormat(date))
    }

}