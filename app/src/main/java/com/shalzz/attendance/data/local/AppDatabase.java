package com.shalzz.attendance.data.local;

import com.shalzz.attendance.data.model.User;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {User.class}, version = 1)
@TypeConverters({TypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
}
