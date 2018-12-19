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

package com.shalzz.attendance.utils;

import android.util.Log;

import com.bugsnag.android.Bugsnag;

import timber.log.Timber;

public final class BugsnagTree extends Timber.Tree {

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        message = String.format("%s %s",
                priorityToString(priority),
                message);

        Bugsnag.leaveBreadcrumb(message);

        if (t != null && priority == Log.ERROR) {
            Bugsnag.notify(t);
        }
    }

    private static String priorityToString(int priority) {
        switch (priority) {
            case Log.ERROR:
                return "E";
            case Log.WARN:
                return "W";
            case Log.INFO:
                return "I";
            case Log.DEBUG:
                return "D";
            default:
                return String.valueOf(priority);
        }
    }
}