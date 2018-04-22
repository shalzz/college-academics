package com.shalzz.attendance;

import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.wrapper.DateHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class performs local unit tests without dependencies on the Android framework
 * For testing methods in the DataManager follow this approach:
 * 1. Stub mock helper classes that your method relies on. e.g. RetrofitServices or DatabaseHelper
 * 2. Test the Observable using TestSubscriber
 * 3. Optionally write a SEPARATE test that verifies that your method is calling the right helper
 * using Mockito.verify()
 */
@RunWith(MockitoJUnitRunner.class)
public class DataManagerTest {

    @Mock
    DatabaseHelper mMockDatabaseHelper;
    @Mock
    PreferencesHelper mMockPreferencesHelper;
    @Mock
    DataAPI mMockDataAPI;
    private DataManager mDataManager;

    @Before
    public void setUp() {
        mDataManager = new DataManager(mMockDataAPI, mMockDatabaseHelper, mMockPreferencesHelper);
    }

    @Test
    public void syncUserEmitsValues() {
        String USERID = "1234567890";
        User user = TestDataFactory.makeUser("u1");
        stubSyncUserHelperCalls(USERID, user);

        TestObserver<User> result = new TestObserver<>();
        mDataManager.syncUser(USERID).subscribe(result);
        result.assertNoErrors();
        result.assertValue(user);
    }

    @Test
    public void syncAttendanceEmitsValues() {
        List<Subject> subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"));
        stubSyncAttendanceHelperCalls(subjects);

        TestObserver<Subject> result = new TestObserver<>();
        mDataManager.syncAttendance().subscribe(result);
        result.assertNoErrors();
        result.assertValueSequence(subjects);
    }

    @Test
    public void syncDayEmitsValues() {
        Date day = new Date();
        List<Period> periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day));
        stubSyncDayHelperCalls(day, periods);

        TestObserver<Period> result = new TestObserver<>();
        mDataManager.syncDay(day).subscribe(result);
        result.assertNoErrors();
        result.assertValueSequence(periods);
    }

    @Test
    public void syncUserCallsApiAndDatabase() {
        String USERID = "1234567890";
        User user = TestDataFactory.makeUser("u1");
        stubSyncUserHelperCalls(USERID, user);

        mDataManager.syncUser(USERID).subscribe();
        // Verify right calls to helper methods
        verify(mMockDataAPI).getUser(USERID);
        verify(mMockDatabaseHelper).setUser(user);
    }

    @Test
    public void syncAttendanceCallsApiAndDatabase() {
        List<Subject> subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"));
        stubSyncAttendanceHelperCalls(subjects);

        mDataManager.syncAttendance().subscribe();
        // Verify right calls to helper methods
        verify(mMockDataAPI).getAttendance();
        verify(mMockDatabaseHelper).setSubjects(subjects);
    }

    @Test
    public void syncDayCallsApiAndDatabase() {
        Date day = new Date();
        List<Period> periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day));
        stubSyncDayHelperCalls(day, periods);

        mDataManager.syncDay(day).subscribe();
        // Verify right calls to helper methods
        verify(mMockDataAPI).getTimetable(DateHelper.toTechnicalFormat(day));
        verify(mMockDatabaseHelper).setPeriods(periods);
    }

    @Test
    public void syncUserDoesNotCallDatabaseWhenApiFails() {
        String USERID = "1234567890";
        when(mMockDataAPI.getUser(USERID))
                .thenReturn(Observable.error(new RuntimeException()));

        TestObserver<User> result = new TestObserver<>();
        mDataManager.syncUser(USERID).subscribe(result);
        // Verify right calls to helper methods
        verify(mMockDataAPI).getUser(USERID);
        result.assertNoValues();
        verify(mMockDatabaseHelper, never()).setUser(ArgumentMatchers.any());
    }

    @Test
    public void syncAttendanceDoesNotCallDatabaseWhenApiFails() {
        when(mMockDataAPI.getAttendance())
                .thenReturn(Observable.error(new RuntimeException()));

        mDataManager.syncAttendance().subscribe(new TestObserver<>());
        // Verify right calls to helper methods
        verify(mMockDataAPI).getAttendance();
        verify(mMockDatabaseHelper, never()).setSubjects(ArgumentMatchers.anyList());
    }

    @Test
    public void syncDayDoesNotCallDatabaseWhenApiFails() {
        Date day = new Date();
        when(mMockDataAPI.getTimetable(DateHelper.toTechnicalFormat(day)))
                .thenReturn(Observable.error(new RuntimeException()));

        mDataManager.syncDay(day).subscribe(new TestObserver<>());
        // Verify right calls to helper methods
        verify(mMockDataAPI).getTimetable(DateHelper.toTechnicalFormat(day));
        verify(mMockDatabaseHelper, never()).setPeriods(ArgumentMatchers.anyList());
    }

    private void stubSyncUserHelperCalls(String userid, User user) {
        // Stub calls to the DataAPI service and database helper.
        when(mMockDataAPI.getUser(userid))
                .thenReturn(Observable.just(user));
        when(mMockDatabaseHelper.setUser(user))
                .thenReturn(Observable.just(user));
    }

    private void stubSyncAttendanceHelperCalls(List<Subject> subjects) {
        // Stub calls to the DataAPI service and database helper.
        when(mMockDataAPI.getAttendance())
                .thenReturn(Observable.just(subjects));
        when(mMockDatabaseHelper.setSubjects(subjects))
                .thenReturn(Observable.fromIterable(subjects));
    }

    private void stubSyncDayHelperCalls(Date date, List<Period> periods) {
        // Stub calls to the DataAPI service and database helper.
        when(mMockDataAPI.getTimetable(DateHelper.toTechnicalFormat(date)))
                .thenReturn(Observable.just(periods));
        when(mMockDatabaseHelper.setPeriods(periods))
                .thenReturn(Observable.fromIterable(periods));
    }

}