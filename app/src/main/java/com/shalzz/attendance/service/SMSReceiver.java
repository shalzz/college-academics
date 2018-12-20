/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import timber.log.Timber;

public class SMSReceiver extends BroadcastReceiver
{
    public static final String SMS_EXTRA_NAME = "pdus";
    public static String SenderName;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Get the SMS map from Intent
        Bundle extras = intent.getExtras();

        if ( extras != null )
        {
            // Get received SMS array
            Object[] smsExtra = (Object[]) extras.get( SMS_EXTRA_NAME );

            for (Object aSmsExtra : smsExtra) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) aSmsExtra);

                String address = sms.getOriginatingAddress();
                String body = sms.getMessageBody();

                Timber.d("Checking for sender: %s", SenderName);
                Timber.d("SMS from: %s, body: %s", address, body);

                if (address.equalsIgnoreCase(SenderName)) {
                    String[] split = body.split("OTP is ");
                    String code = split[1];
                    code = code.substring(0, Math.min(code.length(), 6));

                    Timber.e(new Exception("OTP="+code), "Got OTP for %s", body);

                    Intent myIntent = new Intent("otp");
                    myIntent.putExtra("code", code);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(myIntent);
                }
            }
        }
    }

}