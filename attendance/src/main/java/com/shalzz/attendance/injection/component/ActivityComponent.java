package com.shalzz.attendance.injection.component;

import com.shalzz.attendance.activity.LoginActivity;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.activity.SplashActivity;
import com.shalzz.attendance.fragment.AboutSettingsFragment;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.fragment.ProxySettingsFragment;
import com.shalzz.attendance.fragment.SettingsFragment;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.injection.PerActivity;
import com.shalzz.attendance.injection.module.ActivityModule;

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
}
