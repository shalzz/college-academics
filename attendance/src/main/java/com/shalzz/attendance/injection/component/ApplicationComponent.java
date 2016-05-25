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

package com.shalzz.attendance.injection.component;

import com.shalzz.attendance.activity.LoginActivity;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.activity.SplashActivity;
import com.shalzz.attendance.fragment.AboutSettingsFragment;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.fragment.ProxySettingsFragment;
import com.shalzz.attendance.fragment.SettingsFragment;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.injection.module.ApplicationModule;
import com.shalzz.attendance.injection.module.NetworkModule;
import com.shalzz.attendance.sync.SyncService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class, NetworkModule.class})
public interface ApplicationComponent {

    void inject(SplashActivity activity);
    void inject(LoginActivity activity);
    void inject(MainActivity activity);

    void inject(TimeTablePagerFragment timeTablePagerFragment);

    void inject(AttendanceListFragment attendanceListFragment);

    void inject(AboutSettingsFragment aboutSettingsFragment);

    void inject(ProxySettingsFragment proxySettingsFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(SyncService syncService);
}
