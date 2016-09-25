package com.shalzz.attendance.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * @author shalzz
 */
public class MyBackupAgent extends BackupAgentHelper {
    static final String DATABASE_FILE = "attendanceManager";
    static final String ANALYTICS_DATABASE_FILE = "google_analytics_v4.db";
    static final String PREFS_DEFAULT = "_has_set_default_values.xml";
    static final String PREFS_ADMOB = "admob.xml";
    static final String PREFS_ANALYTICS = "com.google.android.gms.analytics.prefs.xml";
    static final String PREFS_SHARED = "com.shalzz.attendance_preferences.xml";
    static final String PREFS_APP = "SETTINGS.xml";
    static final String PREFS_SHOWCASE = "showcase_internal.xml";

    static final String FILES_BACKUP_KEY = "databases";
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        FileBackupHelper helper = new FileBackupHelper(this, DATABASE_FILE, ANALYTICS_DATABASE_FILE);
        addHelper(FILES_BACKUP_KEY, helper);

        SharedPreferencesBackupHelper prefsHelper =
                new SharedPreferencesBackupHelper(this,
                        PREFS_DEFAULT,
                        PREFS_ADMOB,
                        PREFS_ANALYTICS,
                        PREFS_SHARED,
                        PREFS_APP,
                        PREFS_SHOWCASE);
        addHelper(PREFS_BACKUP_KEY, prefsHelper);
    }
}
