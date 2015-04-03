/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of upes-academics.
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

package com.shalzz.attendance.mute;

import java.util.Calendar;
 
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
 
public class ScheduleService extends Service {
 
    /**
     * Class for clients to access
     */
    public class ServiceBinder extends Binder {
        ScheduleService getService() {
            return ScheduleService.this;
        }
    }
 
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ScheduleService", "Received start id " + startId + ": " + intent);
         
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }
 
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
 
    // This is the object that receives interactions from clients. See
    private final IBinder mBinder = new ServiceBinder();
 
    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public void setAlarm(Calendar c) {
        // This starts a new thread to set the alarm
        // You want to push off your tasks onto a new thread to free up the UI to carry on responding
        new AlarmTask(this, c).run();
    }
}