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

package com.shalzz.attendance.utils;

import android.content.Context;
import androidx.appcompat.widget.SearchView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class Miscellaneous {

    public static class Analytics {
        public static class Event {
            public static final String THEME_CHANGE = "theme_change";
            public static final String LOGIN_INITIATED = "login_initiated";
            public static final String IAP_INITIATED = "iap_initiated";
            public static final String IAP_PURCHASE = "iap_purchase";
        }

        public static class Param {
            public static final String THEME = "theme";
            public static final String USER_ID = "login_user_id";
            public static final String PASSWORD = "login_password";
            public static final String IAP_PRODUCT_ID = "iap_product_id";
        }
    }

    /**
     * Shows the default user soft keyboard.
     * @param editText The view to focus the cursor on.
     */
    public static void showKeyboard(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // will trigger it only if no physical keyboard is open
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Closes the default user soft keyboard.
     * @param context Activity context
     * @param searchView the view supposedly having the cursor focus.
     */
    public static void closeKeyboard(Context context, SearchView searchView) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
    }


    /**
     * Closes the default user soft keyboard.
     * @param context Activity context
     * @param editText the view supposedly having the cursor focus.
     */
    public static void closeKeyboard(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // only will trigger it if no physical keyboard is open
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    /**
     * Material design snack bar
     * @param view the parent view
     * @param msg message to be displayed
     */
    public static void showSnackBar(View view, String msg) {
        Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
                .show();
    }

    public static String capitalizeString(String name) {
        char[] chars = name.toLowerCase(Locale.getDefault()).toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
                found = false;
            }
        }
        return String.valueOf(chars);
    }
}
