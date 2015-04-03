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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;

/**
 * This service is started when an Alarm has been raised
 *
 * We pop a notification into the status bar for the user to click on
 * When the user clicks the notification a new activity is opened
 *
 * @author paul.blundell
 */
public class NotifyService extends Service {

    /**
     * Unique id to identify the notification.
     */
    private static final int NOTIFICATION = 123;

    /**
     * Name of an intent extra we can use to identify if this service was started to create a notification
     */
    public static final String INTENT_NOTIFY = "com.shalzz.attendance.INTENT_NOTIFY";

    /**
     * Name of an intent extra used to determine whether to mute the phone or unmute it.
     */
    public static final String INTENT_MUTE = "com.shalzz.attendance.INTENT_MUTE_UNMUTE";

    /**
     * The string used to store the value of the ring mode set by the system settings
     */
    public static final String PREVIOUS_RING_MODE = "PREVIOUS_RING_MODE";

    /**
     * The system notification manager
     */
    private NotificationManager mNM;

    /**
     * Class for clients to access
     */
    public class ServiceBinder extends Binder {
        NotifyService getService() {
            return NotifyService.this;
        }
    }
 
    @Override
    public void onCreate() {
        Log.i("NotifyService", "onCreate()");
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
 
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
         
        // If this service was started by out AlarmTask intent then we want to show our notification
        if(intent.getBooleanExtra(INTENT_NOTIFY, false)) {
            muteUnMuteRinger(intent.getBooleanExtra(INTENT_MUTE,true));
            showNotification();
        }
         
        // We don't care if this service is stopped as we have already delivered our notification
        return START_NOT_STICKY;
    }
 
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
 
    // This is the object that receives interactions from clients
    private final IBinder mBinder = new ServiceBinder();

    /**
     * Puts the phone to vibrate only or silent mode for a specific period of time
     */
    private void muteUnMuteRinger(boolean mute) {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences settings = getSharedPreferences("SETTINGS", 0);
        SharedPreferences.Editor editor = settings.edit();

        if(mute) {
            int previousMode = mAudioManager.getRingerMode();
            editor.putInt(PREVIOUS_RING_MODE, previousMode).apply();
            int ringMode = Integer.parseInt(sharedPref.getString(
                    getString(R.string.pref_key_ring_mode), "1"));
            mAudioManager.setRingerMode(ringMode);
        }
        else {
            int previousMode = settings.getInt(PREVIOUS_RING_MODE,AudioManager.RINGER_MODE_NORMAL);
            mAudioManager.setRingerMode(previousMode);
        }
    }
 
    /**
     * Creates a notification and shows it in the OS drag-down status bar
     */
    private void showNotification() {
        // This is the 'title' of the notification
        CharSequence title = "Alarm!!";
        // This is the icon to use on the notification
        int icon = R.drawable.alert;
        // This is the scrolling text of the notification
        CharSequence text = "Your notification time is upon us.";      
        // What time to show on the notification
        long time = System.currentTimeMillis();
         
        Notification notification = new Notification(icon, text, time);
 
        // The PendingIntent to launch our activity if the user selects this notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.LAUNCH_FRAGMENT_EXTRA, MainActivity.Fragments.TIMETABLE.getValue());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
 
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, title, text, contentIntent);
 
        // Clear the notification when it is pressed
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
         
        // Send the notification to the system.
        mNM.notify(NOTIFICATION, notification);
         
        // Stop the service when we are finished
        stopSelf();
    }
}