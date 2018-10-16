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
