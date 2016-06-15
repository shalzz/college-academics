package com.shalzz.attendance;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author shalzz
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "com.shalzz.attendance")
public class DatabaseTest {

    @Test
    public void gets_correct_row_count() throws Exception {
        Context context = RuntimeEnvironment.application;
        DatabaseHandler helper = new DatabaseHandler(context);

        assertThat(helper.getUserCount()).isEqualTo(0);
        assertThat(helper.getSubjectCount()).isEqualTo(0);
        assertThat(helper.getPeriodCount()).isEqualTo(0);
    }
}
