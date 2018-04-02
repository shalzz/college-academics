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

package com.shalzz.attendance.ui.timetable;

import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.RxUtil;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class TimeTablePresenter extends BasePresenter<TimeTableMvpView> {

    private DataManager mDataManager;

    private Disposable mDisposable;

    @Inject
    TimeTablePresenter(DataManager dataManager) {
        mDataManager = dataManager;
    }

    @Override
    public void attachView(TimeTableMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mDisposable);
    }

    public void updatePeriods() {
        RxUtil.dispose(mDisposable);
        mDisposable = mDataManager.syncSubjects()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    if(!isViewAttached())
                        return;
                    RetrofitException error = (RetrofitException) throwable;
                    if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                        Timber.e(throwable, error.getMessage());
                        getMvpView().showError(error.getMessage());
                    }
                    else {
                        mDataManager.getSubjectCount()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(count -> {
                                    if (count > 0) {
                                        getMvpView().showError(error.getMessage());
                                    }
                                    else if (error.getKind() == RetrofitException.Kind.HTTP){
                                        getMvpView().showNetworkErrorView(error.getMessage());
                                    }
                                    else if (error.getKind() == RetrofitException.Kind.NETWORK){
                                        getMvpView().showNoConnectionErrorView();
                                    }
                                    else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE){
                                        getMvpView().showEmptyErrorView();
                                    }
                                });
                    }
                })
                .subscribe();
    }
}
