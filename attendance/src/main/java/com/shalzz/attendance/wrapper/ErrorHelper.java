/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
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

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;

public class ErrorHelper {

    public static void handleError(int result, Context mContext) {

        switch (result) {
            case -1:
                Miscellaneous.showMultilineSnackBar(mContext, R.string.session_error);
                break;
            case -2:
                Miscellaneous.showSnackBar(mContext, R.string.unavailable_data_error_msg);
                break;
            case -3:
                Miscellaneous.showSnackBar(mContext, R.string.unavailable_timetable_error_msg);
                break;
            case -4:
                new MaterialDialog.Builder(mContext)
                        .positiveText(android.R.string.ok)
                        .title(R.string.expired_password_error_title)
                        .content(R.string.expired_password_error_msg)
                        .cancelable(false)
                        .show();
                break;
            default:
                break;
        }
    }
}
