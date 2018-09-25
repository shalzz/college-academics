package com.shalzz.attendance.injection.component;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.shalzz.attendance.ui.base.BaseActivity;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.injection.module.ActivityModule;

import javax.inject.Named;

import dagger.Component;

/**
 * A dagger component that will live during the lifecycle of an Activity but it won't
 * be destroy during configuration changes. Check {@link BaseActivity} to see how this components
 * survives configuration changes.
 * Use the {@link ConfigPersistent} scope to annotate dependencies that need to survive
 * configuration changes (for example Presenters).
 */
@ConfigPersistent
@Component(dependencies = ApplicationComponent.class)
public interface ConfigPersistentComponent {

    ActivityComponent activityComponent(ActivityModule activityModule);
}