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