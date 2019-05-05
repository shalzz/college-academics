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

package com.shalzz.attendance.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.bugsnag.android.Bugsnag
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class SyncAdapter
/**
 * Set up the sync adapter. This form of the
 * constructor maintains compatibility with Android 3.0
 * and later platform versions
 */
internal constructor(
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean,
    private val mDataManager: DataManager
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    private var mAccountManager: AccountManager

    private var mAttendanceDisposable: Disposable? = null
    private var mTimetableDisposable: Disposable? = null

    init {
        Bugsnag.setContext("Sync Adapter")
        mAccountManager = AccountManager.get(context)
    }

    override fun onPerformSync(
        account: Account, extras: Bundle, authority: String,
        provider: ContentProviderClient, syncResult: SyncResult
    ) {
        Timber.i("Running sync adapter")

        RxUtil.dispose(mAttendanceDisposable)
        mAttendanceDisposable = mDataManager.syncAttendance()
            .subscribeOn(Schedulers.io())
            .subscribeWith(object : DisposableObserver<Subject>() {
                override fun onNext(subject: Subject) {}

                override fun onError(throwable: Throwable) {
                    val error = throwable as RetrofitException
                    if (error.kind == RetrofitException.Kind.UNEXPECTED) {
                        Timber.e(throwable)
                    }
                }

                override fun onComplete() {
                    RxUtil.dispose(mAttendanceDisposable)
                }
            })

        RxUtil.dispose(mTimetableDisposable)
        mTimetableDisposable = Observable
            .range(-3, 7)
            .concatMap { offset: Int ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DATE, offset)
                Observable.just(calendar.time)
            }
            .concatMap { date -> mDataManager.syncDay(date) }
            .subscribeOn(Schedulers.io())
            .subscribeWith(object : DisposableObserver<Period>() {
                override fun onNext(period: Period) {}

                override fun onError(throwable: Throwable) {
                    val error = throwable as RetrofitException
                    if (error.kind == RetrofitException.Kind.UNEXPECTED) {
                        Timber.e(throwable)
                    }
                }

                override fun onComplete() {
                    RxUtil.dispose(mTimetableDisposable)
                }
            })
    }
}
