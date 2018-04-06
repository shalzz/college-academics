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

package com.shalzz.attendance.ui.day;

import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.RxUtil;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

class DayPresenter extends BasePresenter<DayMvpView> {

    private DataManager mDataManager;

    private Disposable mDisposable;
    private Disposable mNetworkDisposable;
    private Disposable mDbDisposable;

    @Inject
    DayPresenter(DataManager dataManager) {
        mDataManager = dataManager;
    }

    @Override
    public void attachView(DayMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mDisposable);
        RxUtil.dispose(mNetworkDisposable);
        RxUtil.dispose(mDbDisposable);
    }

    public void getDay(Date day) {
        checkViewAttached();
        RxUtil.dispose(mDisposable);
        mDisposable = mDataManager.getPeriodCount(day)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(count -> {
                    if(isViewAttached()) {
                        if (count == 0) {
                            getMvpView().setRefreshing();
                            syncDay(day);
                        }
                        loadDay(day);
                    }
                })
                .doOnComplete(() -> RxUtil.dispose(mDisposable))
                .subscribe();
    }

    public void syncDay(Date day) {
        checkViewAttached();
        RxUtil.dispose(mNetworkDisposable);
        mNetworkDisposable = mDataManager.syncDay(day)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Period>() {
                    @Override
                    public void onNext(Period period) {
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if(!isViewAttached())
                            return;
                        getMvpView().stopRefreshing();
                        RetrofitException error = (RetrofitException) throwable;
                        if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(throwable, error.getMessage());
                            getMvpView().showError(error.getMessage());
                        }
                        else {
                            mDataManager.getPeriodCount(day)
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
                                    })
                                    .subscribe();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (isViewAttached()) {
                            getMvpView().stopRefreshing();
                        }
                    }
                });
    }

    void loadDay(Date day) {
        checkViewAttached();
        RxUtil.dispose(mDbDisposable);
        mDbDisposable = mDataManager.loadDay(day)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Period>>() {
                    @Override
                    public void onNext(List<Period> periods) {
                        if(!isViewAttached())
                            return;
                        if (periods.size() == 0) {
                            getMvpView().clearDay();
                        } else {
                            getMvpView().setDay(periods);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
