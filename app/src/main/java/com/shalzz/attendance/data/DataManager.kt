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

    val subjectCount: Single<Int>
        get() = mDatabaseHelper.subjectCount

    val userCount: Single<Int>
        get() = mDatabaseHelper.userCount

    fun syncAttendance(): Observable<Subject> {
        return mDataAPI.attendance
                .concatMap(mDatabaseHelper::setSubjects)
    }

    fun loadAttendance(filter: String?): Observable<List<Subject>> {
        return mDatabaseHelper.getSubjects(filter)
    }

    fun syncDay(date: Date): Observable<Period> {
        return mDataAPI.getTimetable(DateHelper.toTechnicalFormat(date))
                .concatMap(mDatabaseHelper::setPeriods)
    }

    fun loadDay(date: Date): Observable<List<Period>> {
        return mDatabaseHelper.getPeriods(date)
    }

    fun sendRegID(token: String, auth: String): Observable<Boolean> {
        return mDataAPI.sendRegID(authorization=auth, registerationID=token)
    }

    fun syncUser(auth: String): Observable<User> {
        return mDataAPI.getUser(auth)
                .concatMap(mDatabaseHelper::setUser)
    }

    fun loadUser(): Observable<User> {
        return mDatabaseHelper.user
    }

    fun getPeriodCount(date: Date): Single<Int> {
        return mDatabaseHelper.getPeriodCount(date)
    }

    fun verifyValidSignature(purchase: Purchase): Observable<Boolean> {
        return mDataAPI.verifyValidSignature(purchase.originalJson,
                purchase.signature)
    }

    fun resetTables() {
        mDatabaseHelper.resetTables()
    }
}
