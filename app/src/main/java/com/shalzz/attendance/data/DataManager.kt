package com.shalzz.attendance.data

import com.android.billingclient.api.Purchase
import com.shalzz.attendance.data.local.DatabaseHelper
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.ListFooter
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
            private val mDatabaseHelper: DatabaseHelper,
            private val mPreferencesHelper: PreferencesHelper) {

    val listFooter: Observable<ListFooter>
        get() = mDatabaseHelper.listFooter
                .subscribeOn(Schedulers.single())

    val subjectCount: Single<Int>
        get() = mDatabaseHelper.subjectCount
                .subscribeOn(Schedulers.single())

    val userCount: Single<Int>
        get() = mDatabaseHelper.userCount
                .subscribeOn(Schedulers.single())

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

    fun sendRegID(token: String, auth: String): Observable<Boolean> {
        return mDataAPI.sendRegID(authorization=auth, registerationID=token)
                .subscribeOn(Schedulers.io())
    }

    fun syncUser(auth: String): Observable<User> {
        return mDataAPI.getUser(auth)
                .subscribeOn(Schedulers.io())
                .concatMap(mDatabaseHelper::setUser)
                .subscribeOn(Schedulers.single())
    }

    fun loadUser(): Observable<User> {
        return mDatabaseHelper.user
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
