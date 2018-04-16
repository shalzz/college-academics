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

import android.content.Context;

import com.shalzz.attendance.R;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.NetworkUtil;
import com.shalzz.attendance.utils.RxUtil;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

class DayPresenter extends BasePresenter<DayMvpView> {

    private DataManager mDataManager;
    private Context mContext;

    private Disposable mNetworkDisposable;
    private Disposable mDbDisposable;

    @Inject
    DayPresenter(DataManager dataManager, @ApplicationContext Context context) {
        mDataManager = dataManager;
        mContext = context;
    }

    @Override
    public void attachView(DayMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mNetworkDisposable);
        RxUtil.dispose(mDbDisposable);
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
                            Timber.e(throwable);
                            getMvpView().showError(error.getMessage());
                        }
                        else {
                            Disposable disposable = mDataManager.getPeriodCount(day)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeWith(new DisposableSingleObserver<Integer>() {
                                        @Override
                                        public void onSuccess(Integer count) {
                                            if( !isViewAttached() ) {
                                                //noinspection UnnecessaryReturnStatement
                                                return;
                                            }
                                            else if (!NetworkUtil.isNetworkConnected(mContext)) {
                                                Timber.i("Sync canceled, connection not available");
                                                if (count > 0) {
                                                    getMvpView().showRetryError(
                                                            mContext.getString(R.string.no_internet));
                                                } else {
                                                    getMvpView().showNoConnectionErrorView();
                                                }
                                            }
                                            else if (count > 0) {
                                                getMvpView().showRetryError(error.getMessage());
                                            }
                                            else if (error.getKind() == RetrofitException.Kind.HTTP
                                                    || error.getKind() == RetrofitException.Kind.NETWORK){
                                                getMvpView().showNetworkErrorView(error.getMessage());
                                            }else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE) {
                                                getMvpView().clearDay();
                                                // Prevent recursive calls
                                                mDbDisposable.dispose();
                                            }
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Timber.e(e);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onComplete() {
                        // close any db observables
                        mNetworkDisposable.dispose();
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
                            getMvpView().setRefreshing();
                            syncDay(day);
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
