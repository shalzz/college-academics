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

package com.shalzz.attendance.data

import android.util.Base64
import com.android.billingclient.api.Purchase
import com.shalzz.attendance.data.local.DatabaseHelper
import com.shalzz.attendance.data.model.College
import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.TokenModel
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.data.remote.DataAPI
import com.shalzz.attendance.wrapper.DateHelper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject
constructor(private val mDataAPI: DataAPI,
    private val mDatabaseHelper: DatabaseHelper) {

    val listFooter: Observable<ListFooter>
        get() = mDatabaseHelper.listFooter
            .subscribeOn(Schedulers.single())

    val subjectCount: Single<Int>
        get() = mDatabaseHelper.subjectCount
            .subscribeOn(Schedulers.single())

    val userCount: Single<Int>
        get() = mDatabaseHelper.userCount
            .subscribeOn(Schedulers.single())

    fun colleges(): Observable<List<College>> {
        return mDataAPI.getColleges()
                .subscribeOn(Schedulers.io())
    }

    fun login(username: String, password: String,
              college: String, captcha: String, cookie: String): Observable<TokenModel> {
        val auth = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        return mDataAPI.login("Basic $auth", college, captcha, cookie)
            .subscribeOn(Schedulers.io())
    }

    fun syncAttendance(): Observable<Subject> {
        return mDataAPI.attendance
            .subscribeOn(Schedulers.io())
            .concatMap(mDatabaseHelper::setSubjects)
            .subscribeOn(Schedulers.single())
    }

    fun loadAttendance(filter: String?): Observable<List<Subject>> {
        return mDatabaseHelper.getSubjects(filter)
            .subscribeOn(Schedulers.single())
    }

    fun syncDay(date: Date): Observable<Period> {
        return mDataAPI.getTimetable(DateHelper.toTechnicalFormat(date))
            .subscribeOn(Schedulers.io())
            .concatMap(mDatabaseHelper::setPeriods)
            .subscribeOn(Schedulers.single())
    }

    fun loadDay(date: Date): Observable<List<Period>> {
        return mDatabaseHelper.getPeriods(date)
            .subscribeOn(Schedulers.single())
    }

    fun syncUser(): Observable<User> {
        return mDataAPI.getUser()
            .subscribeOn(Schedulers.io())
            .concatMap(mDatabaseHelper::setUser)
            .subscribeOn(Schedulers.single())
    }

    fun loadUser(username: String): Observable<User> {
        return mDatabaseHelper.getUser(username)
            .subscribeOn(Schedulers.single())
    }

    fun getPeriodCount(date: Date): Single<Int> {
        return mDatabaseHelper.getPeriodCount(date)
            .subscribeOn(Schedulers.single())
    }

    fun verifyValidSignature(purchase: Purchase): Observable<Boolean> {
        return mDataAPI.verifyValidSignature(purchase.originalJson,
            purchase.signature)
            .subscribeOn(Schedulers.io())
    }

    fun resetTables() {
        mDatabaseHelper.resetTables()
    }
}
