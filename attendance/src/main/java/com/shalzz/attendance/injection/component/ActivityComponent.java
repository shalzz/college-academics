package com.shalzz.attendance.injection.component;

import com.shalzz.attendance.injection.PerActivity;
import com.shalzz.attendance.injection.module.ActivityModule;
import com.shalzz.attendance.ui.attendance.AttendanceListFragment;
import com.shalzz.attendance.ui.day.DayFragment;
import com.shalzz.attendance.ui.login.LoginActivity;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.ui.settings.AboutSettingsFragment;
import com.shalzz.attendance.ui.settings.ProxySettingsFragment;
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

    void inject(LoginActivity activity);

    void inject(MainActivity activity);

    void inject(TimeTablePagerFragment timeTablePagerFragment);

    void inject(AttendanceListFragment attendanceListFragment);

    void inject(AboutSettingsFragment aboutSettingsFragment);

    void inject(ProxySettingsFragment proxySettingsFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(DayFragment dayFragment);
}
