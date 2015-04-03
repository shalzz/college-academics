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

package com.shalzz.attendance;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.shalzz.attendance.wrapper.MyVolley;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

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
     * @param context activity context
     * @param msg message to be displayed
     */
    public static void showSnackBar(Context context, String msg) {
        SnackbarManager.show(
                Snackbar.with(context)
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                        .textColor(context.getResources().getColor(R.color.accent))
                        .text(msg), (Activity) context);
    }

    /**
     * Material design snack bar
     * @param context activity context
     * @param msgRes Resource id of the message to be displayed
     */
    public static void showSnackBar(Context context, int msgRes) {
        SnackbarManager.show(
                Snackbar.with(context)
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                        .textColor(context.getResources().getColor(R.color.accent))
                        .text(context.getString(msgRes)), (Activity) context);
    }

    /**
     * Material design snack bar
     * @param context activity context
     * @param msgRes Resourse id of the message to be displayed
     */
    public static void showMultilineSnackBar(Context context, int msgRes) {
        SnackbarManager.show(
                Snackbar.with(context)
                        .type(SnackbarType.MULTI_LINE)
                        .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                        .textColor(context.getResources().getColor(R.color.accent))
                        .text(context.getString(msgRes)), (Activity) context);
    }

	/**
	 * Determines whether to use proxy settings or not.
	 * @return true or false.
	 */
	public static boolean useProxy() {
		ConnectivityManager connManager = (ConnectivityManager) MyVolley.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (mWifi.isConnectedOrConnecting()) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyVolley.getAppContext());
			boolean useProxy = sharedPref.getBoolean("", false);
			final String username = sharedPref.getString(
                    MyVolley.getAppContext().getString(R.string.pref_key_proxy_username), "");
			final String password = sharedPref.getString(
                    MyVolley.getAppContext().getString(R.string.pref_key_proxy_password), "");
			if(useProxy && !username.isEmpty() && !password.isEmpty())
			{
				WifiManager wifiManager = (WifiManager) MyVolley.getAppContext().getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				Log.d("wifiInfo",""+ wifiInfo.toString());
				Log.d("SSID",""+wifiInfo.getSSID());
				Toast.makeText(MyVolley.getAppContext(), "Wifi changed to "+wifiInfo.getSSID(), Toast.LENGTH_LONG).show();
				if (wifiInfo.getSSID().contains("UPESNET"))
				{
					//					OkAuthenticator auth = new OkAuthenticator() {
					//						
					//						@Override
					//						public Credential authenticateProxy(Proxy arg0, URL arg1,
					//								List<Challenge> arg2) throws IOException {
					//							return Credential.basic(username,password);
					//						}
					//						
					//						@Override
					//						public Credential authenticate(Proxy arg0, URL arg1, List<Challenge> arg2)
					//								throws IOException {
					//							return Credential.basic(username,password);
					//						}
					//					};
					//					Authenticator.setDefault((Authenticator) auth);

					Authenticator authenticator = new Authenticator() {

						public PasswordAuthentication getPasswordAuthentication() {
							return (new PasswordAuthentication(username,password.toCharArray()));
						}
					};
					Authenticator.setDefault(authenticator);
					return true;
				}
			}
		}
		return false;
	}
}
