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

package com.shalzz.attendance;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;

import com.shalzz.attendance.data.model.local.AbsentDate;
import com.shalzz.attendance.data.model.local.ListFooter;
import com.shalzz.attendance.data.model.remote.Period;
import com.shalzz.attendance.data.model.remote.Subject;
import com.shalzz.attendance.data.model.remote.User;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper Class for SQLite database
 * @author shalzz
 *
 */
@Singleton
public class DatabaseHandler extends SQLiteOpenHelper {

    /**
     * Database Version
     */
    private static final int DATABASE_VERSION = 9;

    /**
     * Database Name
     */
    private static final String DATABASE_NAME = "attendanceManager";

    /**
     * ListFooter Table Column names
     */
    private static final String KEY_TOTAL_HELD = "Classes_held";
    private static final String KEY_TOTAL_ATTEND = "Classes_attend";

    /**
     * Constructor.
     */
    @Inject
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Create Table.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Subject.CREATE_TABLE);
        db.execSQL(Period.CREATE_TABLE);
        db.execSQL(User.CREATE_TABLE);
        db.execSQL(AbsentDate.CREATE_TABLE);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        }
        db.enableWriteAheadLogging();
    }

    /**
     * Drop the table if it exist and create a new table.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Subject.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Period.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + User.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + AbsentDate.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    /**
     * Add new Subject
     * @param subject the {@link Subject} to add
     */
    public void addSubject(Subject subject, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();

        db.insertWithOnConflict(Subject.TABLE_NAME,null, new Subject.Marshal()
                        .id(subject.id())
                        .name(subject.name())
                        .attended(subject.attended())
                        .held(subject.held())
                        .last_updated(timestamp)
                        .asContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE);

        // Store the dates in another table corresponding to the same id
        for(Date date : subject.absent_dates()) {
            db.insertWithOnConflict(AbsentDate.TABLE_NAME, null, new AbsentDate.Marshal()
                            .subject_id(subject.id())
                            .absent_date(date)
                            .asContentValues(),
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    /**
     * Get All Subjects
     * @return subjectList
     */
    public List<Subject> getAllSubjects(AsyncTaskLoader callback, String filter) {
        List<Subject> subjectList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;

        if (filter != null) {
            cursor = db.rawQuery(Subject.SELECT_LIKE_NAME, new String[] {filter});
        } else {
            cursor = db.rawQuery(Subject.SELECT_ALL,null);
        }
        try {
            while (cursor.moveToNext()) {

                // Check isLoadInBackgroundCanceled() to cancel out early
                if (callback != null && callback.isLoadInBackgroundCanceled()) {
                    break;
                }

                // get absent dates from another table and add in the subject object
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(Subject.ID));
                ArrayList<Date> dates = new ArrayList<>();
                Cursor dateCursor = db.rawQuery(AbsentDate.SELECT_BY_ID,
                        new String[]{String.valueOf(id)});
                while (dateCursor.moveToNext()) {
                    dates.add(AbsentDate.MAPPER.map(dateCursor).absent_date());
                }

                dateCursor.close();
                subjectList.add(Subject.MAPPER.map(cursor).withAbsentDates(dates));
            }
        } finally {
            cursor.close();
        }

        return subjectList;
    }

    @SuppressLint("NewApi")
    public List<Integer> getAbsentSubjects(Date date) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Integer> subjectIDs = new ArrayList<>();

        try (Cursor cursor = db.rawQuery(AbsentDate.SELECT_ABSENT_SUBJECTS,
                new String[]{String.valueOf(DateHelper.formatToTechnicalFormat(date))})) {
            while (cursor.moveToNext()) {
                subjectIDs.add(AbsentDate.MAPPER.map(cursor).subject_id());
            }
        }

        return subjectIDs;
    }

    /**
     * Checks for any obsolete data, based on the timestamp,
     * and deletes if any.
     * @return 1 if one or more subjects are purged else 0
     */
    @SuppressLint("NewApi")
    public int purgeOldSubjects() {
        SQLiteDatabase db = getWritableDatabase();
        try(Cursor cursor = db.rawQuery( Subject.DELETE_OBSOLETE, null)) {
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
    }

    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.insertWithOnConflict(User.TABLE_NAME, null, new User.Marshal()
                        .sap_id(user.sap_id())
                        .name(user.name())
                        .course(user.course())
                        .password(user.password())
                        .asContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    @SuppressLint("NewApi")
    public long getLastSync() {
        SQLiteDatabase db = this.getReadableDatabase();
        try(Cursor cursor = db.rawQuery(Subject.SELECT_LAST_SYNC, null)) {

            if (cursor.moveToFirst()) {
                long now = new Date().getTime();
                long lastSync = cursor.getLong(0);
                return (now - lastSync) / (1000 * 60 * 60);
            }
            cursor.close();
            return -1;
        }
    }

    @SuppressLint("NewApi")
    public User getUser() {
        SQLiteDatabase db = this.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(User.SELECT_ALL, null)) {

            if (cursor.moveToNext()) {
                return User.MAPPER.map(cursor);
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    public ListFooter getListFooter() {
        SQLiteDatabase db = this.getReadableDatabase();
        ListFooter footer = null;

        String selectQuery = "SELECT  sum(" + Subject.ATTENDED+ ") as " + KEY_TOTAL_ATTEND
                + ",sum(" + Subject.HELD+ ") as " + KEY_TOTAL_HELD
                + " FROM " + Subject.TABLE_NAME + ";";
        try(Cursor cursor = db.rawQuery(selectQuery, null)) {

            if (cursor.moveToFirst()) {
                footer = ListFooter.builder()
                        .setHeld(cursor.getFloat(cursor.getColumnIndexOrThrow(KEY_TOTAL_HELD)))
                        .setAttended(cursor.getFloat(cursor.getColumnIndexOrThrow
                                (KEY_TOTAL_ATTEND)))
                        .build();
            }
            db.close();
            cursor.close();
        }

        return footer;
    }

    public void addPeriod(Period period, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.insertWithOnConflict(Period.TABLE_NAME, null, new Period.Marshal()
                        .id(period.id())
                        .name(period.name())
                        .week_day(period.week_day())
                        .teacher(Miscellaneous.capitalizeString(period.teacher()))
                        .room(period.room().trim())
                        .start_time(period.start_time())
                        .end_time(period.end_time())
                        .batch(period.batch())
                        .last_updated(timestamp)
                        .asContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    @SuppressLint("NewApi")
    public ArrayList<Period> getAllPeriods(Date date, AsyncTaskLoader callback) {
        String dayName = DateHelper.getShortWeekday(date);
        ArrayList<Period> periods = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try(Cursor cursor = db.rawQuery(Period.SELECT_BY_WEEK_DAY, new String[] {dayName})) {

            while (cursor.moveToNext()) {
                // Check isLoadInBackgroundCanceled() to cancel out early
                if (callback != null && callback.isLoadInBackgroundCanceled()) {
                    break;
                }
                periods.add(Period.MAPPER.map(cursor));
            }
        }

        return periods;
    }

    /**
     * Checks for any obsolete data, based on the timestamp,
     * and deletes if any.
     * @return 1 if one or more Periods are purged else 0
     */
    @SuppressLint("NewApi")
    public int purgeOldPeriods() {
        SQLiteDatabase db = getWritableDatabase();
        try(Cursor cursor = db.rawQuery( Period.DELETE_OBSOLETE, null)) {
            int count = cursor.getCount();
            cursor.close();
            return count;
        }
    }

    /**
     * Check if the attendance data is in database.
     * */
    @SuppressLint("NewApi")
    public int getSubjectCount() {
        SQLiteDatabase db = getReadableDatabase();
        try(Cursor cursor = db.rawQuery(Subject.SELECT_COUNT, null)) {
            int rowCount = cursor.getCount();
            cursor.close();

            return rowCount;
        }
    }

    /**
     * Check if the Student data is in database.
     * */
    @SuppressLint("NewApi")
    public int getUserCount() {
        SQLiteDatabase db = getReadableDatabase();
        try(Cursor cursor = db.rawQuery(User.SELECT_COUNT, null)) {
            int rowCount = cursor.getCount();
            cursor.close();

            return rowCount;
        }
    }

    @SuppressLint("NewApi")
    public int getPeriodCount() {
        SQLiteDatabase db = getReadableDatabase();
        try(Cursor cursor = db.rawQuery(Period.SELECT_COUNT, null)) {
            int rowCount = cursor.getCount();
            cursor.close();

            return rowCount;
        }
    }

    /**
     * Delete all tables and create them again
     * */
    public void resetTables(){
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete All Rows
        db.delete(Subject.TABLE_NAME, "1", null);
        db.delete(Period.TABLE_NAME, "1", null);
        db.delete(User.TABLE_NAME, "1", null);
        db.delete(AbsentDate.TABLE_NAME, "1", null);
        db.close();
    }
}