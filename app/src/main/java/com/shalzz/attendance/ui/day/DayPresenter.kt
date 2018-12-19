/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.ui.day

import android.content.Context
import com.shalzz.attendance.R
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.NetworkUtil
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class DayPresenter @Inject
internal constructor(private val mDataManager: DataManager,
                     @param:ApplicationContext private val mContext: Context)
    : BasePresenter<DayMvpView>() {

    private var mNetworkDisposable: Disposable? = null
    private var mDbDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: DayMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mNetworkDisposable)
        RxUtil.dispose(mDbDisposable)
    }

    fun syncDay(day: Date) {
        checkViewAttached()
        RxUtil.dispose(mNetworkDisposable)
        mNetworkDisposable = mDataManager.syncDay(day)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Period>() {
                    override fun onNext(period: Period) {}

                    override fun onError(throwable: Throwable) {
                        if (!isViewAttached)
                            return
                        mvpView.stopRefreshing()
                        val error = throwable as RetrofitException
                        if (error.kind == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(throwable)
                            mvpView.showError(error.message)
                        } else {
                            val disposable = mDataManager.getPeriodCount(day)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribeWith(object : DisposableSingleObserver<Int>() {
                                        override fun onSuccess(count: Int) {
                                            if (!isViewAttached) {

                                                return
                                            } else if (!NetworkUtil.isNetworkConnected(mContext)) {
                                                if (count > 0) {
                                                    mvpView.showRetryError(
                                                            mContext.getString(R.string.no_internet))
                                                } else {
                                                    mvpView.showNoConnectionErrorView()
                                                }
                                            } else if (count > 0) {
                                                mvpView.showRetryError(error.message)
                                            } else if (error.kind == RetrofitException.Kind.HTTP || error.kind == RetrofitException.Kind.NETWORK) {
                                                mvpView.showNetworkErrorView(error.message)
                                            } else if (error.kind == RetrofitException.Kind.EMPTY_RESPONSE) {
                                                mvpView.clearDay()
                                                // Prevent recursive calls
                                                mDbDisposable!!.dispose()
                                            }
                                        }

                                        override fun onError(e: Throwable) {
                                            Timber.e(e)
                                        }
                                    })
                        }
                    }

                    override fun onComplete() {
                        // close any db observables
                        mNetworkDisposable!!.dispose()
                    }
                })
    }

    internal fun loadDay(day: Date) {
        checkViewAttached()
        RxUtil.dispose(mDbDisposable)
        mDbDisposable = mDataManager.loadDay(day)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Period>>() {
                    override fun onNext(periods: List<Period>) {
                        if (!isViewAttached)
                            return
                        if (periods.isEmpty()) {
                            mvpView.setRefreshing()
                            syncDay(day)
                        } else {
                            mvpView.setDay(periods)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun onComplete() {}
                })
    }
}
