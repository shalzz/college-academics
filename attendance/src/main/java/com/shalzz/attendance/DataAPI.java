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

package com.shalzz.attendance;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MyStringRequest;
import com.shalzz.attendance.wrapper.MyVolley;

import java.util.HashMap;
import java.util.Map;

public class DataAPI {

	public static void getAttendance(final Context mContext,Response.Listener<String> successListener, Response.ErrorListener errorListener) {

		String mURL = mContext.getString(R.string.URL_attendance);
		MyStringRequest requestAttendance = new MyStringRequest(Method.POST,
				mURL,
				successListener,
				errorListener) {

			public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("User-Agent", mContext.getString(R.string.UserAgent));
				return headers;
			}
        };
		requestAttendance.setShouldCache(true);
		requestAttendance.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		MyVolley.getInstance().addToRequestQueue(requestAttendance ,mContext.getClass().getName());
	}

	public static void getTimeTable(final Context mContext,Response.Listener<String> successListener, Response.ErrorListener errorListener) {

        String mURL = mContext.getString(R.string.URL_timetable);
		MyStringRequest requestTimeTable = new MyStringRequest(Method.POST,
				mURL,
				successListener,
				errorListener) {

			protected Map<String, String> getParams() throws com.android.volley.AuthFailureError {
				Map<String, String> params = new HashMap<String, String>();
				String date = DateHelper.getNetworkRequestDate(DateHelper.getToDay());
				params.put("fromdate", date);
				params.put("submit","Show Result");
				return params;
			}

            public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("User-Agent", mContext.getString(R.string.UserAgent));
				return headers;
			}
        };
		requestTimeTable.setShouldCache(true);
		requestTimeTable.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		MyVolley.getInstance().addToRequestQueue(requestTimeTable ,mContext.getClass().getName());
	}
}
