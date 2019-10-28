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

package com.shalzz.attendance.data.local

import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.wrapper.DateHelper
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper Class for SQLite database
 *
 * @author shalzz
 */
@Singleton
class DatabaseHelper @Inject
constructor(private val mDb: AppDatabase) {

    val listFooter: Observable<ListFooter>
        get() = mDb.subjectDao().getTotalAttendance()

    val subjectCount: Single<Int>
        get() = mDb.subjectDao().getCount()

    val userCount: Single<Int>
        get() = mDb.userDao().getCount()

    fun getUser(username: String): Observable<User> {
        return mDb.userDao().getAllByUsername(username)
    }

    fun setSubjects(newSubjects: Collection<Subject>): Observable<Subject> {
        return Observable.create { source ->
            if (source.isDisposed) return@create

            mDb.runInTransaction {
                mDb.subjectDao().deleteAll()
                for (subject in newSubjects) {
                    mDb.subjectDao().insert(subject)
                    source.onNext(subject)
                }
                source.onComplete()
            }
        }
    }

    fun getSubjects(filter: String?): Observable<List<Subject>> {
        val name = if (filter == null) "%%" else "%$filter%"
        return mDb.subjectDao().getAllLikeName(name)
    }

    fun setPeriods(newPeriods: List<Period>): Observable<Period> {
        return Observable.create { subscriber ->
            if (subscriber.isDisposed) return@create
            if (newPeriods.isEmpty()) return@create

            mDb.runInTransaction {
                mDb.periodDao().deleteByDate(newPeriods[0].date)
                for (period in newPeriods) {
                    mDb.periodDao().insert(period)
                    subscriber.onNext(period)
                }
                subscriber.onComplete()
            }
        }
    }

    fun getPeriods(date: Date): Observable<List<Period>> {
        return mDb.periodDao().getAllByDate(DateHelper.toTechnicalFormat(date))
    }

    fun setUser(user: User): Observable<User> {
        return Observable.create { subscriber ->
            if (subscriber.isDisposed) return@create
            mDb.userDao().insert(user)
            subscriber.onNext(user)
            subscriber.onComplete()
        }
    }

    fun getPeriodCount(day: Date): Single<Int> {
        return mDb.periodDao().getCountByDate(DateHelper.toTechnicalFormat(day))
    }

    /**
     * Delete All Rows
     */
    fun resetTables() : Job  {
        return GlobalScope.launch (Dispatchers.IO) {
            mDb.subjectDao().deleteAll()
            mDb.periodDao().deleteAll()
            mDb.userDao().deleteAll()
        }
    }
}
