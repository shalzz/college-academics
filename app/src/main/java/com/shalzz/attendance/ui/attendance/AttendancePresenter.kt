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

package com.shalzz.attendance.ui.attendance

import android.content.Context
import com.shalzz.attendance.R
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.NetworkUtil
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class AttendancePresenter @Inject
internal constructor(private val mDataManager: DataManager, @param:ApplicationContext private val mContext: Context) : BasePresenter<AttendanceMvpView>() {

    private var mSyncDisposable: Disposable? = null
    private var mDbDisposable: Disposable? = null
    private var mFooterDisposable: Disposable? = null

    override fun attachView(mvpView: AttendanceMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mSyncDisposable)
        RxUtil.dispose(mDbDisposable)
        RxUtil.dispose(mFooterDisposable)
    }

    fun syncAttendance() {
        checkViewAttached()
        RxUtil.dispose(mSyncDisposable)
        mSyncDisposable = mDataManager.syncAttendance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Subject>() {
                    override fun onNext(subject: Subject) {}

                    override fun onError(error: Throwable) {
                        if (error !is RetrofitException) {
                            Timber.e(error)
                        }
                        else if (!isViewAttached)
                            return
                        else if (error.kind == RetrofitException.Kind.UNEXPECTED) {
                            Timber.e(error)
                            mvpView.showError(error.message)
                        } else {
                            val disposable = mDataManager.subjectCount
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe( {count ->
                                            if (!isViewAttached) {
                                                return@subscribe
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
                                                mvpView.showEmptyErrorView()
                                                // Prevent recursive calls
                                                mDbDisposable!!.dispose()
                                            }
                                        }, {
                                            Timber.e(it)
                                        }
                                    )
                        }
                    }

                    override fun onComplete() {
                        mSyncDisposable!!.dispose()
                    }
                })
    }

    fun loadAttendance(filter: String?) {
        checkViewAttached()
        RxUtil.dispose(mDbDisposable)
        mDbDisposable = mDataManager.loadAttendance(filter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Subject>>() {

                    override fun onNext(subjects: List<Subject>) {
                        if (isViewAttached) {
                            // if data is null
                            // sync with network
                            // while showing the loading screen
                            // otherwise load the attendance
                            // as well as the listfooter
                            if (subjects.isEmpty()) {
                                mvpView.setRefreshing()
                                syncAttendance()
                            } else {
                                mvpView.addSubjects(subjects)
                                loadListFooter()
                            }
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun onComplete() {}
                })
    }

    private fun loadListFooter() {
        checkViewAttached()
        RxUtil.dispose(mFooterDisposable)
        mFooterDisposable = mDataManager.listFooter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<ListFooter>() {
                    override fun onNext(footer: ListFooter) {
                        if (isViewAttached) {
                            mvpView.updateFooter(footer)
                        }
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun onComplete() {
                        if (isViewAttached) {
                            mvpView.showcaseView()
                        }
                    }
                })
    }
}
