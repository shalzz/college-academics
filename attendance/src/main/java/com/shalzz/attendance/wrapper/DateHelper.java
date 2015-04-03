/*
 * Copyright (c) 2014 Shaleen Jain <shaleen.jain95@gmail.com>
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

package com.shalzz.attendance.wrapper;

import com.shalzz.attendance.model.Week;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static DateFormat technicalDateFormat = new SimpleDateFormat("dd-MM-yyyy",Locale.US);
    private static DateFormat properDateFormat = new SimpleDateFormat("dd/MM/yyyy",Locale.US);

    public interface DateTimeInterpreter {
        String interpretDate(Calendar date);
        String interpretTime(int hour);
    }

    public static String getTechnicalWeekday(Date date) {
        Calendar today = Calendar.getInstance();
        today.setTime(date);
        int weekday = today.get(Calendar.DAY_OF_WEEK);
        return Week.getTechnical(weekday - 1);
    }

    public static String getProperWeekday(Date date) {
        Calendar today = Calendar.getInstance();
        today.setTime(date);
        int weekday = today.get(Calendar.DAY_OF_WEEK);
        return Week.getProper(weekday-1);
    }

    public static Date getToDay() {
        return new Date();
    }

    public static Date addDays(Date date, int numberOfDays) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, numberOfDays);
        return c.getTime();
    }

    public static String formatToTechnicalFormat(Date date) {
        return technicalDateFormat.format(date);
    }

    public static String formatToProperFormat(Date date) {
        return properDateFormat.format(date);
    }

    public static String getNetworkRequestDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if(c.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)
            date = addDays(date,1);
        return technicalDateFormat.format(date);
    }

    public static String to12HrFormat(String time) throws ParseException {
        DateFormat hr24Format = new SimpleDateFormat("HH:mm");
        DateFormat hr12Format = new SimpleDateFormat("hh:mm aa");
        Date d = hr24Format.parse(time);
        return hr12Format.format(d);
    }

    public static String to24HrFormat(String time) throws ParseException {
        DateFormat hr24Format = new SimpleDateFormat("HH:mm");
        Date d = hr24Format.parse(time);
        return hr24Format.format(d);
    }
}
