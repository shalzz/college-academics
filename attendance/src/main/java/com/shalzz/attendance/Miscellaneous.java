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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class Miscellaneous {

    // Google Analytics Custom Dimensions
    public static final int CUSTOM_DIMENSION_THEME = 1;
    public static final int CUSTOM_DIMENSION_PROXY = 2;

    private MaterialDialog.Builder builder = null;
    private MaterialDialog pd = null;
    private Context mContext;

    @Inject
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

    /**
     * Determines whether to use proxy host or not.
     * @return true or false.
     */
    public boolean useProxy() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean useProxy = sharedPref.getBoolean(mContext.getString(R.string.pref_key_use_proxy), false);
        if(useProxy) {
            ConnectivityManager connManager = (ConnectivityManager) mContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWifi.isConnectedOrConnecting()) {
                WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                Timber.d("Wifi changed to %s", wifiInfo.getSSID());
                return wifiInfo.getSSID().equals(mContext.getString(R.string.upesnet_ssid));
            }
        }
        return false;
    }

    public static ProxySelector getProxySelector() {
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                List<Proxy> list = new ArrayList<>();
                list.add(Proxy.NO_PROXY);
                list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        "proxy.ddn.upes.ac.in",8080)));
                list.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        "proxy1.ddn.upes.ac.in",8080)));
                return list;
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException failure) {

            }
        };
    }
}
