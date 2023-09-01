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

package com.shalzz.attendance.ui.base;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.shalzz.attendance.MyApplication;
import com.shalzz.attendance.injection.component.ActivityComponent;
import com.shalzz.attendance.injection.component.ConfigPersistentComponent;
import com.shalzz.attendance.injection.module.ActivityModule;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import timber.log.Timber;

/**
 * Abstract activity that every other Activity in this application must implement. It handles
 * creation of Dagger components and makes sure that instances of ConfigPersistentComponent survive
 * across configuration changes.
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private static final String KEY_ACTIVITY_ID = "KEY_ACTIVITY_ID";
    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private static final Map<Long, ConfigPersistentComponent> sComponentsMap = new HashMap<>();

    private ActivityComponent mActivityComponent;
    private long mActivityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the ActivityComponent and reuses cached ConfigPersistentComponent if this is
        // being called after a configuration change.
//        mActivityId = savedInstanceState != null ?
//                savedInstanceState.getLong(KEY_ACTIVITY_ID) : NEXT_ID.getAndIncrement();
//        ConfigPersistentComponent configPersistentComponent;
//        if (!sComponentsMap.containsKey(mActivityId)) {
//            Timber.i("Creating new ConfigPersistentComponent id=%d", mActivityId);
//            configPersistentComponent = DaggerConfigPersistentComponent.builder()
//                    .applicationComponent(MyApplication.get(this).getComponent())
//                    .build();
//            sComponentsMap.put(mActivityId, configPersistentComponent);
//        } else {
//            Timber.i("Reusing ConfigPersistentComponent id=%d", mActivityId);
//            configPersistentComponent = sComponentsMap.get(mActivityId);
//        }
//        mActivityComponent = configPersistentComponent.activityComponent(new ActivityModule(this));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_ACTIVITY_ID, mActivityId);
    }

    @Override
    protected void onDestroy() {
        if (!isChangingConfigurations()) {
            Timber.i("Clearing ConfigPersistentComponent id=%d", mActivityId);
            sComponentsMap.remove(mActivityId);
        }
        super.onDestroy();
    }

    public ActivityComponent activityComponent() {
        return mActivityComponent;
    }

}
