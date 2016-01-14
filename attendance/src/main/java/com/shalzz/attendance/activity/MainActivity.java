/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.fragment.SettingsFragment;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.model.UserModel;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MyVolley;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity {

    /**
     * Reference to fragment positions
     */
    public enum Fragments {
        ATTENDANCE(1),
        TIMETABLE(2),
        SETTINGS(3);

        private final int value;

        private Fragments(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Drawer lock state. True for tablets, false otherwise .
     */
    private boolean isDrawerLocked = false;

    /**
     * To prevent saving the drawer position when logging out.
     */
    public static boolean LOGGED_OUT = false;

    /**
     * Remember the position of the selected item.
     */
    public static final String PREFERENCE_ACTIVATED_FRAGMENT = "ACTIVATED_FRAGMENT2.2";

    public static final String FRAGMENT_TAG = "MainActivity.FRAGMENT";

    public static final String LAUNCH_FRAGMENT_EXTRA = "MainActivity.LAUNCH_FRAGMENT";

    private static final String PREVIOUS_FRAGMENT_TAG = "MainActivity.PREVOIUS_FRAGMENT";

    private static final String mTag = "MainActivity";

    /**
     * Debug flag
     */
    private final boolean DEBUG_FRAGMENTS = true;
    public boolean mPopSettingsBackStack =  false;

    // Views
    @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.list_slidermenu)
    NavigationView mNavigationView;

    private int mCurrentSelectedPosition = Fragments.ATTENDANCE.getValue();
    private String[] mNavTitles;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private View Drawerheader;
    private FragmentManager mFragmentManager;
    private Fragment fragment = null;
    private ActionBar actionbar;
    // Our custom poor-man's back stack which has only one entry at maximum.
    private Fragment mPreviousFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);
        ButterKnife.inject(this);

        // set toolbar as actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNavTitles = getResources().getStringArray(R.array.drawer_array);
        mFragmentManager = getFragmentManager();
        mTitle  = getTitle();
        actionbar = getSupportActionBar();

        // Check for tablet layout
//        FrameLayout frameLayout = (FrameLayout)findViewById(R.id.frame_container);
//        if(((ViewGroup.MarginLayoutParams)frameLayout.getLayoutParams()).leftMargin == (int)getResources().getDimension(R.dimen.drawer_size)) {
//            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mNavigationView);
//            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
//            isDrawerLocked = true;
//        }

        Drawerheader = mNavigationView.inflateHeaderView(R.layout.drawer_header);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionbar.setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                actionbar.setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the list's click listener
        mNavigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener());
        mDrawerToggle.setDrawerIndicatorEnabled(true);

        // Set the drawer toggle as the DrawerListener
        if(!isDrawerLocked) {
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        // Select either the default item (Fragments.ATTENDANCE) or the last selected item.
        mCurrentSelectedPosition = reloadCurrentFragment();

        // Recycle fragment
        if(savedInstanceState != null) {
            fragment =  getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            mPreviousFragment = getFragmentManager().getFragment(savedInstanceState, PREVIOUS_FRAGMENT_TAG);
            Log.d(mTag, "current fag found: " + fragment );
            Log.d(mTag, "previous fag found: " + mPreviousFragment );
            showFragment(fragment);
            selectItem(mCurrentSelectedPosition);
        }
        else if(getIntent().hasExtra(LAUNCH_FRAGMENT_EXTRA)) {
            displayView(getIntent().getIntExtra(LAUNCH_FRAGMENT_EXTRA,
                    Fragments.ATTENDANCE.getValue()));
        }
        else if(getIntent().getAction()!=null &&
                getIntent().getAction().equals(Intent.ACTION_MANAGE_NETWORK_USAGE)) {
            displayView(Fragments.SETTINGS.getValue());
        }
        else {
            displayView(mCurrentSelectedPosition);
        }

        updateDrawerHeader();
        showcaseView();
    }

    void showcaseView() {
        if(MyPreferencesManager.isFirstLaunch(mTag)) {

            Target homeTarget = new Target() {
                @Override
                public Point getPoint() {
                    // Get approximate position of home icon's center
                    int actionBarSize = getSupportActionBar().getHeight();
                    int x = actionBarSize / 2;
                    int y = actionBarSize / 2;
                    return new Point(x, y);
                }
            };

            final ShowcaseView sv = new ShowcaseView.Builder(this)
                    .setTarget(homeTarget)
                    .setStyle(R.style.ShowcaseTheme)
                    .setContentTitle(getString(R.string.sv_main_activity_title))
                    .setContentText(getString(R.string.sv_main_activity_content))
                    .build();

            sv.overrideButtonClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sv.hide();
                    if(fragment instanceof AttendanceListFragment)
                    {
                        ((AttendanceListFragment) fragment).showcaseView();
                    }
                }
            });
            MyPreferencesManager.setFirstLaunch(mTag);
        }
    }

    public void updateDrawerHeader() {
        DatabaseHandler db = new DatabaseHandler(this);
        if(db.getUserCount()>0) {
            UserModel user = db.getUser();

            TextView tv_name = (TextView) Drawerheader.findViewById(R.id.drawer_header_name);
            TextView tv_course = (TextView) Drawerheader.findViewById(R.id.drawer_header_course);
            TextView last_refresh = (TextView) Drawerheader.findViewById(R.id.last_refreshed);
            tv_name.setText(user.getName());
            tv_course.setText(user.getCourse());

            // TODO: separate sync and user
            // TODO: use a view holder
            int time = (int) db.getLastSync();
            last_refresh.setText(getResources().getQuantityString(R.plurals.tv_last_refresh, time, time));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                mDrawerLayout.closeDrawer(mNavigationView);
            } else {
                mDrawerLayout.openDrawer(mNavigationView);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    private class NavigationItemSelectedListener implements NavigationView.OnNavigationItemSelectedListener {
        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            displayView(menuItem.getOrder());
            return false;
        }
    }

    void displayView(int position) {
        // update the main content by replacing fragments
        switch (position) {
            case 0:
                return;
            case 1:
                fragment = new AttendanceListFragment();
                break;
            case 2:
                fragment = new TimeTablePagerFragment();
                break;
            case 3:
                fragment = new SettingsFragment();
                break;
            default:
                break;
        }

        if (fragment != null) {
            showFragment(fragment);
            selectItem(position);
            mPopSettingsBackStack = false;
        } else {
            Log.e(mTag, "Error in creating fragment");
        }
    }

    /**
     * Update selected item and title, then close the drawer
     * @param position the item to highlight
     */
    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        mNavigationView.getMenu().getItem(position-1).setChecked(true);
        mDrawerTitle = mNavTitles[position-1];
        setTitle(mDrawerTitle);
        if(!isDrawerLocked && mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.closeDrawer(mNavigationView);
    }

    /**
     * Push the installed fragment into our custom back stack (or optionally
     * {@link FragmentTransaction#remove} it) and {@link FragmentTransaction#add} {@code fragment}.
     *
     * @param fragment {@link Fragment} to be added.
     *
     */
    private void showFragment(Fragment fragment) {
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        final Fragment installed = getInstalledFragment();

        // return if the fragment is already installed
        if(isAttendanceListInstalled() && fragment instanceof AttendanceListFragment ||
                isTimeTablePagerInstalled() && fragment instanceof TimeTablePagerFragment ||
                isSettingsInstalled() && fragment instanceof SettingsFragment) {
            return;
        }

        if (mPreviousFragment != null) {
            if (DEBUG_FRAGMENTS) {
                Log.d(mTag, this + " showFragment: destroying previous fragment "
                        + mPreviousFragment.getClass().getSimpleName());
            }
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.remove(mPreviousFragment);
            mPreviousFragment = null;
        }

        // Remove the current fragment and push it into the backstack.
        if (installed != null) {
            mPreviousFragment = installed;
            ft.detach(mPreviousFragment);
        }

        // Show the new one
        ft.add(R.id.frame_container,fragment,FRAGMENT_TAG);
        if(fragment instanceof SettingsFragment)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        else
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        // close drawer if it is open
        if (mDrawerLayout.isDrawerOpen(mNavigationView) && !isDrawerLocked)
        {
            mDrawerLayout.closeDrawer(mNavigationView);
        }
        else if (shouldPopFromBackStack()) {
            if(mPopSettingsBackStack) {
                mPopSettingsBackStack = false;
                getFragmentManager().popBackStackImmediate();
            } else {
                // Custom back stack
                popFromBackStack();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    /**
     * @return true if we should pop from our custom back stack.
     */
    private boolean shouldPopFromBackStack() {

        if (mPreviousFragment == null) {
            return false; // Nothing in the back stack
        }
        final Fragment installed = getInstalledFragment();
        if (installed == null) {
            // If no fragment is installed right now, do nothing.
            return false;
        }
        // Okay now we have 2 fragments; the one in the back stack and the one that's currently
        // installed.
        return !(installed instanceof AttendanceListFragment ||
                installed instanceof TimeTablePagerFragment);

    }

    /**
     * Pop from our custom back stack.
     */
    private void popFromBackStack() {
        if (mPreviousFragment == null) {
            return;
        }
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        final Fragment installed = getInstalledFragment();
        int position = Fragments.ATTENDANCE.getValue() ;
        Log.i(mTag, this + " backstack: [pop] " + installed.getClass().getSimpleName() + " -> "
                + mPreviousFragment.getClass().getSimpleName());

        ft.remove(installed);
        ft.attach(mPreviousFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        ft.commit();

        // redraw fragment
        if (mPreviousFragment instanceof AttendanceListFragment) {
            position = Fragments.ATTENDANCE.getValue();
        } else if (mPreviousFragment instanceof TimeTablePagerFragment) {
            position = Fragments.TIMETABLE.getValue();
            //((TimeTablePagerFragment) mPreviousFragment).updateFragmentsData();
            // fixme: this is a crude hack for viewpager not redrawing itself
            showFragment(new TimeTablePagerFragment());
        }
        selectItem(position);
        mPreviousFragment = null;
    }

    private void persistCurrentFragment() {
        if(!LOGGED_OUT) {
            SharedPreferences.Editor editor = getSharedPreferences("SETTINGS", 0).edit();
            mCurrentSelectedPosition = mCurrentSelectedPosition == Fragments.SETTINGS.getValue() ?
                    Fragments.ATTENDANCE.getValue() : mCurrentSelectedPosition;
            editor.putInt(PREFERENCE_ACTIVATED_FRAGMENT, mCurrentSelectedPosition).commit();
        }
    }

    private int reloadCurrentFragment() {
        SharedPreferences settings = getSharedPreferences("SETTINGS", 0);
        return settings.getInt(PREFERENCE_ACTIVATED_FRAGMENT, Fragments.ATTENDANCE.getValue());
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        actionbar.setTitle(mTitle);
        actionbar.setSubtitle("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    /**
     * @return currently installed {@link Fragment} (1-pane has only one at most), or null if none
     *         exists.
     */
    private Fragment getInstalledFragment() {
        return mFragmentManager.findFragmentByTag(FRAGMENT_TAG);
    }

    private boolean isAttendanceListInstalled() {
        return getInstalledFragment() instanceof AttendanceListFragment;
    }

    private boolean isTimeTablePagerInstalled() {
        return getInstalledFragment() instanceof TimeTablePagerFragment;
    }

    private boolean isSettingsInstalled() {
        return getInstalledFragment() instanceof SettingsFragment;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // for orientation changes, etc.
        if (mPreviousFragment != null) {
            getFragmentManager()
                    .putFragment(outState, PREVIOUS_FRAGMENT_TAG, mPreviousFragment);
            Log.d(mTag, "previous fag saved: " + mPreviousFragment.getClass().getSimpleName());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
        actionbar.setTitle(mTitle);
    }

    @Override
    public void onPause() {
        persistCurrentFragment();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        MyVolley.getInstance().cancelPendingRequests("com.shalzz.attendance.activity.MainActivity");
        super.onDestroy();
    }

}