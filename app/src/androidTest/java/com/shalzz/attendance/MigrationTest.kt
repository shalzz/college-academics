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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.shalzz.attendance.data.local.AppDatabase
import com.shalzz.attendance.data.model.entity.Subject
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Arrays

@RunWith(AndroidJUnit4::class)
@SmallTest
class MigrationTest {

    @get:Rule
    var helper: MigrationTestHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory())

    @Test
    @Throws(IOException::class)
    fun migrate12To13() {
        var db = helper.createDatabase(TEST_DB_NAME, 12)

        // db has schema version 12. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        val subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"))
        subjects.forEach { it: Subject -> insertSubject(it, db) }

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB_NAME, 13, true,
                AppDatabase.MIGRATION_12_13)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        // verify that the data is correct
        val dbSubjects = TestObserver<List<Subject>>()
        getMigratedRoomDatabase().subjectDao().getAllLikeName("%%").subscribe(dbSubjects)
        dbSubjects.onNext(subjects)
    }

    private fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
                AppDatabase::class.java, TEST_DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_12_13)
                .build()
        // close the database and release any stream resources when the test finishes
        helper.closeWhenFinished(database)
        return database
    }

    private fun insertSubject(subject: Subject, db: SupportSQLiteDatabase) {
        val values = ContentValues()
        values.put("id", subject.id)
        values.put("name", subject.name)
        values.put("attended", subject.attended)
        values.put("held", subject.held)
        values.put("absent_dates", subject.absentDatesAsString)

        db.insert("Subject", SQLiteDatabase.CONFLICT_REPLACE, values)
    }

    companion object {
        private val TEST_DB_NAME = "migration-test"
    }
}
