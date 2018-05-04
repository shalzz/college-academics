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

package com.shalzz.attendance.data.local;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shalzz.attendance.data.model.AbsentDate;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.injection.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

public class DbOpenHelper extends SupportSQLiteOpenHelper.Callback {

    public static final String DATABASE_NAME = "academics.db";

    public static final int DATABASE_VERSION = 10;

    public DbOpenHelper() {
        super(DATABASE_VERSION);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
        db.execSQL(Subject.CREATE_TABLE);
        db.execSQL(Period.CREATE_TABLE);
        db.execSQL(User.CREATE_TABLE);
        db.execSQL(AbsentDate.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            default:

                // Drop older table if existed
                db.execSQL("DROP TABLE IF EXISTS " + Subject.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + Period.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + User.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + AbsentDate.TABLE_NAME);

                // Create tables again
                onCreate(db);
                break;
        }
    }
}