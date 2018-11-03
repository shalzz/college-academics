package com.shalzz.attendance.injection.component;

import com.shalzz.attendance.injection.PerActivity;
import com.shalzz.attendance.injection.module.ActivityModule;
import com.shalzz.attendance.ui.attendance.AttendanceListFragment;
import com.shalzz.attendance.ui.day.DayFragment;
import com.shalzz.attendance.ui.login.AuthenticatorActivity;
import com.shalzz.attendance.ui.login.LoginFragment;
import com.shalzz.attendance.ui.login.OTPFragment;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.ui.settings.AboutSettingsFragment;
import com.shalzz.attendance.ui.settings.SettingsFragment;
import com.shalzz.attendance.ui.splash.SplashActivity;
import com.shalzz.attendance.ui.timetable.TimeTablePagerFragment;

import org.jetbrains.annotations.NotNull;

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

    void inject(AboutSettingsFragment aboutSettingsFragment);

    void inject(SettingsFragment settingsFragment);

    void inject(DayFragment dayFragment);

    void inject(LoginFragment loginFragment);

    void inject(OTPFragment otpFragment);
}
