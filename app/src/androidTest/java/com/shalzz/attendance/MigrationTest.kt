package com.shalzz.attendance

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.shalzz.attendance.data.local.AppDatabase
import com.shalzz.attendance.data.model.entity.Subject
import io.reactivex.observers.TestObserver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*


@RunWith(AndroidJUnit4::class)
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
        subjects.forEach { insertSubject(it, db) }

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
        val database = Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
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
