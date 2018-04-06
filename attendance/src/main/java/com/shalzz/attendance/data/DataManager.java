package com.shalzz.attendance.data;

import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.model.Day;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;

@Singleton
public class DataManager {

    private final DataAPI mDataAPI;
    private final DatabaseHelper mDatabaseHelper;
    private final PreferencesHelper mPreferencesHelper;

    @Inject
    DataManager(DataAPI dataAPI, DatabaseHelper db, PreferencesHelper prefs) {
        mDataAPI = dataAPI;
        mDatabaseHelper = db;
        mPreferencesHelper = prefs;
    }

    public Observable<Subject> getAttendance() {
        return mDataAPI.getAttendance()
                .concatMap(subjects ->
                        RxJavaInterop.toV2Observable(mDatabaseHelper.setSubjects(subjects)));
    }

    public Observable<List<Subject>> loadAttendance(String filter) {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getSubjects(filter));
    }

    public Observable<Period> getDay(Date date) {
        return mDataAPI.getTimetable(DateHelper.formatToTechnicalFormat(date))
                .concatMap(periods ->
                        RxJavaInterop.toV2Observable(mDatabaseHelper.addPeriods(periods)));
    }

    public Observable<List<Period>> loadDay(Date date) {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getPeriods(date));
    }

    public Observable<User> getUser(String auth) {
        return mDataAPI.getUser(auth)
                .concatMap(user -> RxJavaInterop.toV2Observable(mDatabaseHelper.addUser(user)));
    }

    public Observable<User> loadUser() {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getUser());
    }

    public Observable<ListFooter> getListFooter() {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getListFooter());
    }

    public Observable<Integer> getSubjectCount() {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getSubjectCount());
    }

    public Observable<Integer> getPeriodCount(Date date) {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getPeriodCount(date));
    }

    public Observable<Integer> getUserCount() {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getUserCount());
    }

    public void resetTables() {
        mDatabaseHelper.resetTables();
    }
}
