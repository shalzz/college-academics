package com.shalzz.attendance;

import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.util.DefaultConfig;
import com.shalzz.attendance.util.RxSchedulersOverrideRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;

import io.reactivex.observers.TestObserver;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = DefaultConfig.EMULATE_SDK)
public class DatabaseHelperTest {

    @Rule
    public final RxSchedulersOverrideRule mOverrideSchedulersRule = new RxSchedulersOverrideRule();

    private DatabaseHelper mDatabaseHelper;

    @Before
    public void setup() {
        if (mDatabaseHelper == null)
            mDatabaseHelper = new DatabaseHelper(RuntimeEnvironment.application,
                    mOverrideSchedulersRule.getScheduler());
    }

    @Test
    public void getUserCount() throws Exception {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getUserCount().subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }

    @Test
    public void getSubjectCount() throws Exception {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getSubjectCount().subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }

    @Test
    public void getPeriodCount() throws Exception {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getPeriodCount(new Date()).subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }
}
