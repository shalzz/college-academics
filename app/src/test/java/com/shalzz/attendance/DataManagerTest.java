package com.shalzz.attendance;

import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.data.remote.DataAPI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    public void syncUserDoesNotCallDatabaseWhenApiFails() {
        String USERID = "1234567890";
        when(mMockDataAPI.getUser(USERID))
                .thenReturn(Observable.error(new RuntimeException()));

        mDataManager.syncUser(USERID).subscribe(new TestObserver<>());
        // Verify right calls to helper methods
        verify(mMockDataAPI).getUser(USERID);
        verify(mMockDatabaseHelper, never()).setUser(ArgumentMatchers.any());
    }

    private void stubSyncUserHelperCalls(String userid, User user) {
        // Stub calls to the DataAPI service and database helper.
        when(mMockDataAPI.getUser(userid))
                .thenReturn(Observable.just(user));
        when(mMockDatabaseHelper.setUser(user))
                .thenReturn(Observable.just(user));
    }

}