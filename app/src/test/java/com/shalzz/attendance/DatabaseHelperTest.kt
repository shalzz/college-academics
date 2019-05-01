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

package com.shalzz.attendance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shalzz.attendance.data.local.AppDatabase
import com.shalzz.attendance.data.local.DatabaseHelper
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.util.DefaultConfig
import com.shalzz.attendance.util.RxSchedulersOverrideRule
import io.reactivex.observers.TestObserver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Arrays
import java.util.Date

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DefaultConfig.EMULATE_SDK])
class DatabaseHelperTest {

    @Rule @JvmField
    val mOverrideSchedulersRule = RxSchedulersOverrideRule()

    private lateinit var mDatabaseHelper: DatabaseHelper
    private lateinit var mDb : AppDatabase

    @Before
    fun setup() {
        mDb = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mDatabaseHelper = DatabaseHelper(mDb)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun writeUserAndReadUser() {
        val user = TestDataFactory.makeUser("u1")

        val writeResult = TestObserver<User>()
        mDatabaseHelper.setUser(user).subscribe(writeResult)
        writeResult.assertNoErrors()
        writeResult.assertValue(user)

        val readResult = TestObserver<User>()
        mDatabaseHelper.getUser(user.username).subscribe(readResult)
        readResult.assertNoErrors()
        readResult.onNext(user) // Since this is reactive streams, onComplete will never be called.
    }

    @Test
    fun writeAndReadSubjects() {
        val subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
            TestDataFactory.makeSubject("s2"))

        mDatabaseHelper.setSubjects(subjects).subscribe()

        val result = TestObserver<List<Subject>>()
        mDatabaseHelper.getSubjects(null).subscribe(result)
        result.assertNoErrors()
        result.onNext(subjects)
    }

    @Test
    fun writeAndReadPeriods() {
        val day = Date()
        val periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
            TestDataFactory.makePeriod("p2", day))

        mDatabaseHelper.setPeriods(periods).subscribe()

        val result = TestObserver<List<Period>>()
        mDatabaseHelper.getPeriods(day).subscribe(result)
        result.assertNoErrors()
        result.onNext(periods)
    }

    @Test
    fun getUserCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.userCount.subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }

    @Test
    fun getSubjectCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.subjectCount.subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }

    @Test
    fun getPeriodCount() {
        val result = TestObserver<Int>()
        mDatabaseHelper.getPeriodCount(Date()).subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }

    @Test
    fun resetAllTables() {
        val day = Date()
        val periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
            TestDataFactory.makePeriod("p2", day))

        mDatabaseHelper.setPeriods(periods).subscribe()

        runBlocking {
            mDatabaseHelper.resetTables().join()
        }

        val result = TestObserver<Int>()
        mDatabaseHelper.getPeriodCount(Date()).subscribe(result)
        result.assertNoErrors()
        result.assertValue(0)
    }
}
