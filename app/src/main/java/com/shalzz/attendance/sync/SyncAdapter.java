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

package com.shalzz.attendance.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.utils.RxUtil;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final DataManager mDataManager;
    private final PreferencesHelper mPreferencesHelper;

    private Disposable mAttendanceDisposable;
    private Disposable mTimetableDisposable;

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs,
            DataManager dataManager,
            PreferencesHelper preferencesHelper) {
        super(context, autoInitialize, allowParallelSyncs);

		/*
		 * If your app uses a content resolver, get an instance of it
		 * from the incoming Context
		 */
        mDataManager = dataManager;
        mPreferencesHelper = preferencesHelper;
        Bugsnag.setContext("Sync Adapter");
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Timber.i("Running sync adapter");
        Bugsnag.setUserId(mPreferencesHelper.getUserId());

        RxUtil.dispose(mAttendanceDisposable);
        mAttendanceDisposable = mDataManager.syncAttendance()
                .subscribeOn(Schedulers.io())
                .subscribeWith(new DisposableObserver<Subject>() {
                    @Override
                    public void onNext(Subject subject) { }

                    @Override
                    public void onError(Throwable throwable) {
                        RetrofitException error = (RetrofitException) throwable;
                        if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(throwable);
                        }
                    }

                    @Override
                    public void onComplete() {
                        RxUtil.dispose(mAttendanceDisposable);
                    }
                });

        RxUtil.dispose(mTimetableDisposable);
        mTimetableDisposable = Observable
                .range(-3, 7)
                .concatMap((Function<Integer, ObservableSource<Date>>) offset -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DATE, offset);
                    return Observable.just(calendar.getTime());
                })
                .concatMap(mDataManager::syncDay)
                .subscribeOn(Schedulers.io())
                .subscribeWith(new DisposableObserver<Period>() {
                    @Override
                    public void onNext(Period period) { }

                    @Override
                    public void onError(Throwable throwable) {
                        RetrofitException error = (RetrofitException) throwable;
                        if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(throwable);
                        }
                    }

                    @Override
                    public void onComplete() {
                        RxUtil.dispose(mTimetableDisposable);
                    }
                });
    }
}
