package com.shalzz.attendance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shalzz.attendance.data.model.dao.PeriodDao
import com.shalzz.attendance.data.model.dao.SubjectDao
import com.shalzz.attendance.data.model.dao.UserDao
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.injection.ApplicationContext
import javax.inject.Singleton

@Singleton
@Database(entities = [User::class, Subject::class, Period::class], version = 13)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun periodDao(): PeriodDao

    companion object {
        const val DATABASE_NAME = "academics.db"

        fun getInstance(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(context,
                    AppDatabase::class.java, DATABASE_NAME)
                    .addMigrations(MIGRATION_12_13)
                    .fallbackToDestructiveMigrationFrom(10, 11)
                    .build()
        }

        /**
         * Migrate from:
         * version 12 - using Room where the {@link Subject#absent_dates} can be NULL
         * to
         * version 13 - using Room where the {@link Subject#absent_dates} is not a NULL with
         *              default ""
         */
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val TABLE_NAME = "Subject"
                val TEMP_TABLE = "Subject2"
                // SQLite supports a limited operations for ALTER.
                // Changing the type of a column is not directly supported, so this is what we need
                // to do:
                // Create the new table
                db.execSQL("CREATE TABLE $TEMP_TABLE \n" +
                        " (`id` INTEGER NOT NULL,\n" +
                        " `name` TEXT NOT NULL,\n" +
                        " `attended` REAL NOT NULL,\n" +
                        " `held` REAL NOT NULL,\n" +
                        " `absent_dates` TEXT NOT NULL DEFAULT \"\",\n" +
                        " PRIMARY KEY(`id`))")
                // Copy the data
                db.execSQL("INSERT INTO $TEMP_TABLE (id, name, attended, held, absent_dates) " +
                        "SELECT id, name, attended, held, absent_dates FROM " + TABLE_NAME)
                // Remove the old table
                db.execSQL("DROP TABLE $TABLE_NAME")
                // Change the table name to the correct one
                db.execSQL("ALTER TABLE $TEMP_TABLE RENAME TO $TABLE_NAME")
            }
        }
    }
}
