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

import android.content.Context;

import com.shalzz.attendance.R;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.NetworkUtil;
import com.shalzz.attendance.utils.RxUtil;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ConfigPersistent
public class AttendancePresenter extends BasePresenter<AttendanceMvpView> {

    private DataManager mDataManager;
    private Context mContext;

    private Disposable mSyncDisposable;
    private Disposable mDbDisposable;
    private Disposable mFooterDisposable;

    @Inject
    AttendancePresenter(DataManager dataManager, @ApplicationContext Context context) {
        mDataManager = dataManager;
        mContext = context;
    }

    @Override
    public void attachView(AttendanceMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mSyncDisposable);
        RxUtil.dispose(mDbDisposable);
        RxUtil.dispose(mFooterDisposable);
    }

    public void syncAttendance() {
        checkViewAttached();
        RxUtil.dispose(mSyncDisposable);
        mSyncDisposable = mDataManager.syncAttendance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Subject>() {
                    @Override
                    public void onNext(Subject subject) { }

                    @Override
                    public void onError(Throwable throwable) {
                        if(!isViewAttached())
                            return;
                        RetrofitException error = (RetrofitException) throwable;
                        if (error.getKind() == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(throwable);
                            getMvpView().showError(error.getMessage());
                        }
                        else {
                            Disposable disposable = mDataManager.getSubjectCount()
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
                                            } else if (error.getKind() == RetrofitException.Kind.EMPTY_RESPONSE) {
                                                getMvpView().showEmptyErrorView();
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
                        mSyncDisposable.dispose();
                    }
                });
    }

    public void loadAttendance(String filter) {
        checkViewAttached();
        RxUtil.dispose(mDbDisposable);
        mDbDisposable = mDataManager.loadAttendance(filter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Subject>> () {
                    @Override
                    public void onNext(List<Subject> subjects) {
                        if (isViewAttached()) {
                            // if data is null
                            // sync with network
                            // while showing the loading screen
                            // otherwise load the attendance
                            // as well as the listfooter
                            if (subjects.size() == 0) {
                                getMvpView().setRefreshing();
                                syncAttendance();
                            } else {
                                getMvpView().addSubjects(subjects);
                                loadListFooter();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {
                        if (isViewAttached()) {
                            getMvpView().showcaseView();
                        }
                    }
                });
    }

    public void loadListFooter() {
        checkViewAttached();
        RxUtil.dispose(mFooterDisposable);
        mFooterDisposable = mDataManager.getListFooter()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ListFooter> () {
                    @Override
                    public void onNext(ListFooter footer) {
                        if (isViewAttached()) {
                            getMvpView().updateFooter(footer);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {
                        if (isViewAttached()) {
                            getMvpView().showcaseView();
                        }
                    }
                });
    }
}
