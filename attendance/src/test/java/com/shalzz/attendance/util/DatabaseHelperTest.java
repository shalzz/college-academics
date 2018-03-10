package com.shalzz.attendance.util;

import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.data.local.DbOpenHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = DefaultConfig.EMULATE_SDK)
public class DatabaseHelperTest {

    private final DbOpenHelper mDatabaseHelper =
            new DbOpenHelper(RuntimeEnvironment.application);

    @Test
    public void gets_correct_row_count() throws Exception {

        assertEquals(mDatabaseHelper.getUserCount(), 0);
        assertEquals(mDatabaseHelper.getSubjectCount(), 0);
        assertEquals(mDatabaseHelper.getPeriodCount(), 0);
    }
}
