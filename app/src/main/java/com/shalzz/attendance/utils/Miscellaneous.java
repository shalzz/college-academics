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
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.shalzz.attendance.injection.ActivityContext;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.inject.Inject;

public class Miscellaneous {

    // Google Analytics Custom Dimensions
    public static final int CUSTOM_DIMENSION_THEME = 1;
    public static final int CUSTOM_DIMENSION_PROXY = 2;
    public static final int CUSTOM_DIMENSION_USER_ID = 3;

    private MaterialDialog.Builder builder = null;
    private MaterialDialog pd = null;
    private Context mContext;

    @Inject
    public Miscellaneous(@ActivityContext Context context) {
        mContext = context;
    }

    /**
     * Shows the default user soft keyboard.
     * @param mTextView The view to focus the cursor on.
     */
    public static void showKeyboard(Context context, EditText mTextView) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // will trigger it only if no physical keyboard is open
            imm.showSoftInput(mTextView, 0);
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

    /**
     * Calculate md5 for any given string
     * @param s the string
     * @return the hash of the string s
     */
    public static String md5(String s) {
        MessageDigest m;

        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }

        m.update(s.getBytes(),0,s.length());
        String hashtext = new BigInteger(1,m.digest()).toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while(hashtext.length() < 32 ){
            hashtext = "0"+hashtext;
        }
        return hashtext;
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
