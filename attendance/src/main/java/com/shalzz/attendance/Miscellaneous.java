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

package com.shalzz.attendance;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.shalzz.attendance.wrapper.MyVolley;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Miscellaneous {

    private MaterialDialog.Builder builder = null;
    private MaterialDialog pd = null;
    private Context mContext;

    public Miscellaneous(Context context) {
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
     * Displays the default Progress Dialog.
     * @param mMessage The message to display
     */
    public void showProgressDialog(String mMessage , boolean cancelable, DialogInterface.OnCancelListener progressDialogCancelListener) {
        // lazy initialise
        if(pd==null)
        {
            // Setup the Progress Dialog
            pd = new MaterialDialog.Builder(mContext)
                    .content(mMessage)
                    .cancelable(cancelable)
                    .autoDismiss(false)
                    .cancelListener(progressDialogCancelListener)
                    .progress(true, 0)
                    .build();
        }
        pd.show();
    }

    /**
     * Dismisses the Progress Dialog.
     */
    public void dismissProgressDialog() {
        if(pd!=null)
            pd.dismiss();
    }

    /**
     * Displays a basic Alert Dialog.
     * @param mMessage the message to display
     */
    public void showAlertDialog(String mMessage) {
        // lazy initialise
        if(builder==null) {
            builder = new MaterialDialog.Builder(mContext)
                    .cancelable(true)
                    .positiveText(android.R.string.ok);
        }
        dismissProgressDialog();
        builder.content(mMessage)
                .show();
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

    /**
     * Determines whether to use proxy host or not.
     * @return true or false.
     */
    public static boolean useProxy() {
        Resources resources = MyVolley.getMyResources();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyVolley.getAppContext());
        boolean useProxy = sharedPref.getBoolean(resources.getString(R.string.pref_key_use_proxy), false);
        if(useProxy) {
            ConnectivityManager connManager = (ConnectivityManager) MyVolley.getAppContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnectedOrConnecting()) {
                WifiManager wifiManager = (WifiManager) MyVolley.getAppContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                Log.d("Proxy","Wifi changed to " + wifiInfo.getSSID());
                return wifiInfo.getSSID().equals(resources.getString(R.string.upesnet_ssid));
            }
        }
        return false;
    }
    public static String getProxyCredentials() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyVolley.getAppContext());
        final String username = sharedPref.getString(
                MyVolley.getAppContext().getString(R.string.pref_key_proxy_username), "");
        final String password = sharedPref.getString(
                MyVolley.getAppContext().getString(R.string.pref_key_proxy_password), "");
        return Credentials.basic(username, password);
    }

    public static Authenticator getAuthenticator() {
        return new Authenticator() {

            @Override
            public Request authenticate(Proxy proxy, Response response) throws IOException {
                return null;
            }

            @Override
            public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                return response.request().newBuilder()
                        .header("Proxy-Authorization", getProxyCredentials())
                        .build();
            }
        };
    }
}
