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

package com.shalzz.attendance.injection.component;

import com.shalzz.attendance.injection.PerActivity;
import com.shalzz.attendance.injection.module.ActivityModule;
import com.shalzz.attendance.ui.attendance.AttendanceListFragment;
import com.shalzz.attendance.ui.day.DayFragment;
import com.shalzz.attendance.ui.login.AuthenticatorActivity;
import com.shalzz.attendance.ui.login.LoginFragment;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.ui.settings.SettingsFragment;
import com.shalzz.attendance.ui.splash.SplashActivity;
import com.shalzz.attendance.ui.timetable.TimeTablePagerFragment;

import dagger.Subcomponent;

/**
 * This component inject dependencies to all Activities across the application
 */
@PerActivity
@Subcomponent(modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(SplashActivity activity);

    void inject(AuthenticatorActivity activity);

    void inject(MainActivity activity);

    void inject(TimeTablePagerFragment timeTablePagerFragment);

    void inject(AttendanceListFragment attendanceListFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(DayFragment dayFragment);

    void inject(LoginFragment loginFragment);
}
