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

package com.shalzz.attendance.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.DbOpenHelper;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.utils.RxUtil;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ConfigPersistent
public class MainPresenter extends BasePresenter<MainMvpView> {

    private DataManager mDataManager;
    private PreferencesHelper mPreferenceHelper;

    private Disposable mDisposable;
    private Context mContext;

    @Inject
    @Named("app")
    Tracker mTracker;

    @Inject
    MainPresenter(DataManager dataManager,
                  PreferencesHelper preferencesHelper,
                  @ApplicationContext Context context) {
        mDataManager = dataManager;
        mPreferenceHelper = preferencesHelper;
        mContext = context;
    }

    @Override
    public void attachView(MainMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mDisposable);
    }

    public void loadUser() {
        RxUtil.dispose(mDisposable);
        mDisposable = mDataManager.loadUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<User>() {
                    @Override
                    public void onNext(User user) {
                        if (isViewAttached()) {
                            getMvpView().updateUserDetails(user);
                        }
                        Bugsnag.setUserId(user.id());

                        SharedPreferences sharedPref =
                                PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean optIn = sharedPref.getBoolean(mContext.getString(
                                R.string.pref_key_bugsnag_opt_in), true);
                        if(optIn) {
                            Bugsnag.setUserName(user.name());
                            Bugsnag.addToTab("User", "phone", user.phone());
                            Bugsnag.addToTab("User", "email", user.email());
                        }

                        mTracker.set("&uid", user.phone());
                        mTracker.send(new HitBuilders.ScreenViewBuilder()
                                .setCustomDimension(Miscellaneous.CUSTOM_DIMENSION_USER_ID,
                                        user.phone())
                                .build());
                    }

                    @Override
                    public void onError(Throwable e) {
                        RetrofitException error = (RetrofitException) e;
                        Timber.e(e, error.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void logout() {
        checkViewAttached();
        MainActivity.LOGGED_OUT = true;

        // Remove User Details from Shared Preferences.
        mPreferenceHelper.removeUser();

        // Remove user Attendance data from database.
        mDataManager.resetTables();

        getMvpView().logout();
    }
}
