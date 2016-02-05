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

package com.shalzz.attendance.model;

public enum Week {

    SUNDAY ("Sun","Sunday"),
    MONDAY ("Mon","Monday"),
    TUESDAY ("Tue","Tuesday"),
    WEDNESDAY ("Wed","Wednesday"),
    THURSDAY ("Thu","Thursday"),
    FRIDAY ("Fri","Friday"),
    SATURDAY ("Sat","Saturday");

    private final String shortDay;
    private final String fullDay;

    Week(String t, String p) {
        shortDay = t;
        fullDay = p;
    }

    public String getShortDay() {
        return shortDay;
    }

    public static String getShortDay(int i) {
        return Week.values()[i].getShortDay();
    }

    public String getFullDay() {
        return fullDay;
    }

    public static String getFullDay(int i) {
        return Week.values()[i].getFullDay();
    }
}