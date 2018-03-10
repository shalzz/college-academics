package com.shalzz.attendance.data;

import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.local.Day;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.utils.Miscellaneous;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Observable;
import okhttp3.Credentials;

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

    public Observable<Subject> syncSubjects() {
        return mDataAPI.getAttendance()
                .concatMap(subjects ->
                        RxJavaInterop.toV2Observable(mDatabaseHelper.setSubjects(subjects)));
    }

    public Observable<List<Subject>> getSubjects(String filter) {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getSubjects(filter));
    }

    public Observable<Day> getDay(Date date) {
        return RxJavaInterop.toV2Observable(mDatabaseHelper.getAbsentSubjects(date)
                .zipWith(mDatabaseHelper.getPeriods(date),
                        Day::create));
    }

    public Observable<User> getUser(String auth) {
        return mDataAPI.getUser(auth)
                .concatMap(user -> {
                    mPreferencesHelper.saveUser(user.name(), user.password());
                    return RxJavaInterop.toV2Observable(mDatabaseHelper.addUser(user));
                });
    }
}
