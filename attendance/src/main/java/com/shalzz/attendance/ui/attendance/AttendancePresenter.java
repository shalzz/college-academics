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

package com.shalzz.attendance.ui.attendance;

import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.ui.base.BasePresenter;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ConfigPersistent
public class AttendancePresenter extends BasePresenter<AttendanceMvpView> {

    private DataManager mDataManager;

    private Disposable mSyncDisposable;
    private Disposable mDisposable;

    @Inject
    AttendancePresenter(DataManager dataManager) {
        mDataManager = dataManager;
    }

    @Override
    public void attachView(AttendanceMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        mSyncDisposable.dispose();
        mDisposable.dispose();
    }

    public void syncSubjects() {
        mSyncDisposable.dispose();
        mSyncDisposable = mDataManager.syncSubjects()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if(isViewAttached()) {
                        getMvpView().updateLastSync();
                    }
                })
                .doOnError(throwable -> {
                    if(!isViewAttached())
                        return;
                    RetrofitException error = (RetrofitException) throwable;
                    if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                        Timber.e(throwable, error.getMessage());
                        getMvpView().showError(error.getMessage());
                    }
                    else if( /* mDb.getSubjectCount() > 0 && */ false) {
                        getMvpView().showRetryError(error.getMessage());
                    }
                    else if (error.getKind() == RetrofitException.Kind.NETWORK){
                        getMvpView().showNetworkErrorView();
                    }
                    else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE){
                        getMvpView().showEmptyErrorView();
                    }
                })
                .subscribe();
    }

    public void loadSubjects(String filter) {
        checkViewAttached();
        mDisposable.dispose();
        mDisposable = mDataManager.getSubjects(filter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Subject>> () {
                    @Override
                    public void onNext(List<Subject> subjects) {
                        getMvpView().addSubjects(subjects);
                    }

                    @Override
                    public void onError(Throwable e) {
                        RetrofitException error = (RetrofitException) e;
                        Timber.e(e, error.getMessage());
                        getMvpView().showError(error.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        getMvpView().showcaseView();
                    }
                });
    }
}
