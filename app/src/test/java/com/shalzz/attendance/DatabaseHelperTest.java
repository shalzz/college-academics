package com.shalzz.attendance;

import android.database.Cursor;

import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.model.entity.Period;
import com.shalzz.attendance.data.model.entity.Subject;
import com.shalzz.attendance.data.model.entity.User;
import com.shalzz.attendance.util.DefaultConfig;
import com.shalzz.attendance.util.RxSchedulersOverrideRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.observers.TestObserver;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricTestRunner.class)
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
    public void setUser() {
        User user = TestDataFactory.makeUser("u1");

        TestObserver<User> result = new TestObserver<>();
        mDatabaseHelper.setUser(user).subscribe(result);
        result.assertNoErrors();
        result.assertValue(user);

        Cursor cursor = mDatabaseHelper.getBriteDb()
                .query("SELECT * FROM " + User.TABLE_NAME);

        assertEquals(1, cursor.getCount());
        cursor.moveToNext();
        assertEquals(user, User.Companion.getMAPPER().map(cursor));
    }

    @Test
    public void getUser() {
        User user = TestDataFactory.makeUser("u1");

        mDatabaseHelper.setUser(user).subscribe();

        TestObserver<User> result = new TestObserver<>();
        mDatabaseHelper.getUser().subscribe(result);
        result.assertNoErrors();
        result.assertValue(user);
    }

    @Test
    public void getSubjects() {
        List<Subject> subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"));

        mDatabaseHelper.setSubjects(subjects).subscribe();

        TestObserver<List<Subject>> result = new TestObserver<>();
        mDatabaseHelper.getSubjects(null).subscribe(result);
        result.assertNoErrors();
        result.assertValue(subjects);
    }

    @Test
    public void getPeriods() {
        Date day = new Date();
        List<Period> periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day));

        mDatabaseHelper.setPeriods(periods).subscribe();

        TestObserver<List<Period>> result = new TestObserver<>();
        mDatabaseHelper.getPeriods(day).subscribe(result);
        result.assertNoErrors();
        result.assertValue(periods);
    }

    @Test
    public void getUserCount() {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getUserCount().subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }

    @Test
    public void getSubjectCount() {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getSubjectCount().subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }

    @Test
    public void getPeriodCount() {
        TestObserver<Integer> result = new TestObserver<>();
        mDatabaseHelper.getPeriodCount(new Date()).subscribe(result);
        result.assertNoErrors();
        result.assertValue(0);
    }
}
