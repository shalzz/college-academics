package com.shalzz.attendance

import com.shalzz.attendance.data.local.AppDatabase

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.io.IOException

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    var helper: MigrationTestHelper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory())

    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        var db = helper.createDatabase(TEST_DB, 12)

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
//        db.execSQL(...);

        // Prepare for the next version.
//        db.close()

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
//        db = helper.runMigrationsAndValidate(TEST_DB, 2, true,
//                AppDatabase.MIGRATION_10_11)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    companion object {
        private val TEST_DB = "migration-test"
    }
}
