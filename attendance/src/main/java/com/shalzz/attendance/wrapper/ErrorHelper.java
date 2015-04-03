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
