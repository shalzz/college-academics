package com.shalzz.attendance.data;

import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;


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

    public Observable<Subject> syncAttendance() {
        return mDataAPI.getAttendance()
                .concatMap(mDatabaseHelper::setSubjects);
    }

    public Observable<List<Subject>> loadAttendance(String filter) {
        return mDatabaseHelper.getSubjects(filter);
    }

    public Observable<Period> syncDay(Date date) {
        return mDataAPI.getTimetable(DateHelper.formatToTechnicalFormat(date))
                .concatMap(mDatabaseHelper::addPeriods);
    }

    public Observable<List<Period>> loadDay(Date date) {
        return mDatabaseHelper.getPeriods(date);
    }

    public Observable<User> getUser(String auth) {
        return mDataAPI.getUser(auth)
                .concatMap(mDatabaseHelper::addUser);
    }

    public Observable<User> loadUser() {
        return mDatabaseHelper.getUser();
    }

    public Observable<ListFooter> getListFooter() {
        return mDatabaseHelper.getListFooter();
    }

    public Single<Integer> getSubjectCount() {
        return mDatabaseHelper.getSubjectCount().first(0);
    }

    public Single<Integer> getPeriodCount(Date date) {
        return mDatabaseHelper.getPeriodCount(date).first(0);
    }

    public Single<Integer> getUserCount() {
        return mDatabaseHelper.getUserCount().first(0);
    }

    public void resetTables() {
        mDatabaseHelper.resetTables();
    }
}
