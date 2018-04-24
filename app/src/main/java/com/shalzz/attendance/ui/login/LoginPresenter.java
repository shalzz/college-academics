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

package com.shalzz.attendance.ui.login;

import android.content.Context;

import com.shalzz.attendance.R;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.RetrofitException;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.NetworkUtil;
import com.shalzz.attendance.utils.RxUtil;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ConfigPersistent
public class LoginPresenter extends BasePresenter<LoginMvpView> {

    private DataManager mDataManager;
    private Context mContext;

    private Disposable mDisposable;

    @Inject
    LoginPresenter(DataManager dataManager, @ApplicationContext Context context) {
        mDataManager = dataManager;
        mContext = context;
    }

    @Override
    public void attachView(LoginMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mDisposable);
    }

    public void login(final String username) {
        checkViewAttached();
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            Timber.i("Sync canceled, connection not available");
            getMvpView().showError(mContext.getString(R.string.no_internet));
            return;
        }

        getMvpView().showProgressDialog();
        RxUtil.dispose(mDisposable);
        mDisposable = mDataManager.syncUser("Bearer " + username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<User>() {
                    @Override
                    public void onNext(User user) {
                        getMvpView().showMainActivity(user);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) {
                            RetrofitException error = (RetrofitException) e;
                            getMvpView().showError(error.getMessage());
                            Timber.e(e);
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
