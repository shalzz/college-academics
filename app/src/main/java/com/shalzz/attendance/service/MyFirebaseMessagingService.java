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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.shalzz.attendance.MyApplication;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.ui.main.MainActivity;

import java.util.Map;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import timber.log.Timber;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Inject
    DataManager mDataManager;

    @Inject
    PreferencesHelper mPreferencesHelper;

    @Override
    public void onCreate() {
        MyApplication.get(this).getComponent().inject(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0 && mPreferencesHelper.getLoginStatus()) {
            Timber.d("Message data payload: %s", remoteMessage.getData());
            sendNotification(remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Timber.d("Message Notification Body: %s", remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(String token) {
        // Ignore the regId registered with the default firebase project/sender id
        Timber.d("Refreshed regId: %s", token);
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param data FCM message body received.
     */
    private void sendNotification(Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_human)
                .setContentTitle(data.get("title"))
                .setContentInfo(data.get("message"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}