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

package com.shalzz.attendance.runner;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.KeyguardManager;
import android.os.PowerManager;
import androidx.test.runner.AndroidJUnitRunner;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.*;

/**
 * Extension of AndroidJUnitRunner that adds some functionality to unblock the device screen
 * before starting the tests.
 */
public class UnlockDeviceAndroidJUnitRunner extends AndroidJUnitRunner {

    private PowerManager.WakeLock mWakeLock;

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        Application application = (Application) getTargetContext().getApplicationContext();
        String simpleName = UnlockDeviceAndroidJUnitRunner.class.getSimpleName();
        // Unlock the device so that the tests can input keystrokes.
        ((KeyguardManager) application.getSystemService(KEYGUARD_SERVICE))
                .newKeyguardLock(simpleName)
                .disableKeyguard();
        // Wake up the screen.
        PowerManager powerManager = ((PowerManager) application.getSystemService(POWER_SERVICE));
        mWakeLock = powerManager.newWakeLock(FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP |
                ON_AFTER_RELEASE, simpleName);
        mWakeLock.acquire();
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
    }
}
