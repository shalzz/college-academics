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