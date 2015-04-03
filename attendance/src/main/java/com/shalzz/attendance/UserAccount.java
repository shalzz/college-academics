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
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Request.Priority;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.activity.LoginActivity;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MyStringRequest;
import com.shalzz.attendance.wrapper.MySyncManager;
import com.shalzz.attendance.wrapper.MyVolley;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;


public class UserAccount {

    private String mUsername;
    private String mPassword;
    private String mCaptcha;
    private int retryCount = 0;
    private Miscellaneous misc;

    /**
     * The activity context used to Log the user from
     */
    private Context mContext;

    /**
     * The name used to set and get the username(sapid or enrollment no) intent extra
     */
    public static final String INTENT_EXTRA_USERNAME = "SAPID";

    /**
     * Constructor to set the Activity context.
     * @param context Context
     */
    public UserAccount(Context context) {
        mContext = context;
        misc =  new Miscellaneous(mContext);
    }

    /**
     * Sends the login request and saves the user details.
     * @param username Username
     * @param password Password
     * @param captcha Captcha
     * @param data Additional hidden data
     */
    public void Login(final String username, final String password, final String captcha, final Map<String, String> data) {

        mUsername = username;
        mPassword = password;
        mCaptcha = captcha;

        misc.showProgressDialog("Logging in...", false, pdCancelListener());
        String mURL = mContext.getResources().getString(R.string.URL_login);
        MyStringRequest request = new MyStringRequest(Method.POST,
                mURL,
                loginSuccessListener(),
                myErrorListener()) {

            public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("User-Agent", mContext.getString(R.string.UserAgent));
                return headers;
            }

            protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {
                data.put("username", username);
                data.put("passwd", password);
                data.put("txtCaptcha", captcha);
                data.put("submit", "Login");
                data.put("remember", "yes");
                return data;
            }
        };
        request.setShouldCache(false);
        request.setPriority(Priority.HIGH);
        request.setRetryPolicy(new DefaultRetryPolicy(1500, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyVolley.getInstance().addToRequestQueue(request,mContext.getClass().getName());
    }

    private Response.Listener<String> loginSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Resources resources = mContext.getResources();
                String session_error_identifier = resources.getString(R.string.session_error_identifier);
                String http_tag_title = resources.getString(R.string.http_tag_title);
                Document document = Jsoup.parse(response);
                String script = document.getElementsByTag("script").get(0).html();

                if(script.equals(mContext.getString(R.string.incorrect_captcha)))
                {
                    misc.showAlertDialog(mContext.getString(R.string.alert_incorrect_captcha));
                }
                else if(script.equals(mContext.getString(R.string.incorrect_user_or_pass)))
                {
                    misc.showAlertDialog(mContext.getString(R.string.alert_incorrect_password));
                }
                else if(document.getElementsByTag(http_tag_title).size()==0 ||
                        document.getElementsByTag(http_tag_title).text().equals(session_error_identifier))
                {
                    if(retryCount<2)
                    {
                        LoginWithNewHiddenData();
                        retryCount++;
                    }
                    else if(retryCount==2)
                    {
                        new MyPreferencesManager(mContext).removePersistenCookies();
                        LoginWithNewHiddenData();
                    }
                    else
                    {
                        misc.dismissProgressDialog();
                        Miscellaneous.showSnackBar(mContext, R.string.general_try_again_error);
                    }
                }
                else
                {
                    MyPreferencesManager settings = new MyPreferencesManager(mContext);
                    settings.savePersistentCookies();
                    // Used for future re-logins
                    settings.saveUser(mUsername, mPassword);

                    misc.dismissProgressDialog();
                    Intent ourIntent = new Intent(mContext, MainActivity.class);
                    ourIntent.putExtra(INTENT_EXTRA_USERNAME, mUsername);
                    mContext.startActivity(ourIntent);
                    ((Activity) mContext).finish();
                }
            }
        };
    }

    /**
     * Sends the Logout request, clears the user details preferences and deletes all user attendance data.
     */
    public void Logout() {

        misc.showProgressDialog("Logging out...", true, pdCancelListener());
        Bugsnag.leaveBreadcrumb("Logging out...");

        String mURL = mContext.getResources().getString(R.string.URL_logout);
        MyStringRequest request = new MyStringRequest(Method.POST,
                mURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Bugsnag.leaveBreadcrumb("Successfully Logged out...");
                    }
                },
                myErrorListener()) {

            public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("User-Agent", mContext.getString(R.string.UserAgent));
                return headers;
            }

            protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("submit", "Logout");
                params.put("option", "logout");
                params.put("op2", "logout");
                params.put("lang", "english");
                params.put("return", mContext.getResources().getString(R.string.URL_home));
                params.put("message", "0");
                return params;
            }
        };
        request.setShouldCache(false);
        request.setPriority(Priority.IMMEDIATE);
        request.setRetryPolicy(new DefaultRetryPolicy(1500, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyVolley.getInstance().addToRequestQueue(request,"LOGOUT");

        MainActivity.LOGGED_OUT = true;

        // Remove User Details from Shared Preferences.
        MyPreferencesManager settings = new MyPreferencesManager(mContext);
        settings.removeUser();

        // Remove user Attendance data from database.
        DatabaseHandler db = new DatabaseHandler(mContext);
        db.resetTables();

        // Remove Sync Account
        MySyncManager.removeSyncAccount(mContext);

        // Destroy current activity and start Login Activity
        misc.dismissProgressDialog();
        Intent ourIntent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(ourIntent);
        ((Activity) mContext).finish();
    }

    /**
     * Progress Dialog cancel Listener.
     * @return OnCancelListener
     */
    DialogInterface.OnCancelListener pdCancelListener() {
        return new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Cancel all pending requests when user presses back button.
                MyVolley.getInstance().cancelPendingRequests(mContext.getClass().getName());
                MyVolley.getInstance().cancelPendingRequests("LOGOUT");
            }
        };

    }

    /**
     * Logins in with new hidden data in case previous data is corrupted.
     */
    private void LoginWithNewHiddenData()
    {
        Bugsnag.leaveBreadcrumb("Collecting hidden data...");
        String mURL = mContext.getResources().getString(R.string.URL_home);
        MyStringRequest request = new MyStringRequest(Method.GET,
                mURL,
                getHiddenDataSuccessListener(),
                myErrorListener()) {

            public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("User-Agent", mContext.getString(R.string.UserAgent));
                return headers;
            }
        };
        request.setShouldCache(false);
        request.setPriority(Priority.HIGH);
        request.setRetryPolicy(new DefaultRetryPolicy(1500, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyVolley.getInstance().addToRequestQueue(request,mContext.getClass().getName());
    }

    private Response.Listener<String> getHiddenDataSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Bugsnag.leaveBreadcrumb("Collected hidden data.");
                Document doc = Jsoup.parse(response);
                Bugsnag.leaveBreadcrumb("Parsing hidden data...");

                // Get Hidden values
                Map<String, String> data = new HashMap<String, String>();
                Elements hiddenValues = doc.select(mContext.getString(R.string.selector_hidden_data));
                for(Element hiddenValue : hiddenValues)
                {
                    String name = hiddenValue.attr("name");
                    String val = hiddenValue.attr("value");
                    if(name.length()!=0 && val.length()!=0)
                    {
                        data.put(name, val);
                    }
                }
                Bugsnag.leaveBreadcrumb("Parsed hidden data.");
                Login(mUsername, mPassword, mCaptcha, data);
            }
        };
    }

    private Response.ErrorListener myErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                misc.dismissProgressDialog();
                Miscellaneous.showSnackBar(mContext,msg);
                Log.e(mContext.getClass().getName(), msg);
            }
        };
    }
}
