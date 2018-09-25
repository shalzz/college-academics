package com.shalzz.attendance.data.local

import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.wrapper.DateHelper
import java.util.Date

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.Single

/**
 * Helper Class for SQLite database
 *
 * @author shalzz
 */
@Singleton
class DatabaseHelper @Inject
constructor(private val mDb: AppDatabase) {

    val user: Observable<User>
        get() = mDb.userDao().getAll()

    val listFooter: Observable<ListFooter>
        get() = mDb.subjectDao().getTotalAttendance()

    val subjectCount: Single<Int>
        get() = mDb.subjectDao().getCount()

    val userCount: Single<Int>
        get() = mDb.userDao().getCount()

    fun setSubjects(newSubjects: Collection<Subject>): Observable<Subject> {
        return Observable.create { source ->
            if (source.isDisposed) return@create

            mDb.beginTransaction()
            try {
                mDb.subjectDao().deleteAll()
                for (subject in newSubjects) {
                    mDb.subjectDao().insert(subject)
                    source.onNext(subject)
                }
                mDb.setTransactionSuccessful()
            } finally {
                mDb.endTransaction()
                source.onComplete()
            }
        }
    }

    fun getSubjects(filter: String?): Observable<Subject> {
        val name = if (filter == null) "" else "%$filter%"
        return mDb.subjectDao().getAllLikeName(name)
    }

    fun setPeriods(newPeriods: List<Period>): Observable<Period> {
        return Observable.create { subscriber ->
            if (subscriber.isDisposed) return@create
            if (newPeriods.isEmpty()) return@create

            mDb.beginTransaction()
            try {
                mDb.periodDao().deleteByDate(newPeriods[0].date)
                for (period in newPeriods) {
                    mDb.periodDao().insert(period)
                    subscriber.onNext(period)
                }
                mDb.setTransactionSuccessful()
            } finally {
                mDb.endTransaction()
                subscriber.onComplete()
            }
        }
    }

    fun getPeriods(date: Date): Observable<Period> {
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
    fun resetTables() {
        mDb.subjectDao().deleteAll()
        mDb.periodDao().deleteAll()
        mDb.userDao().deleteAll()
    }
}
