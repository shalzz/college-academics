/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.fragment.AttendanceListFragment;
import com.shalzz.attendance.fragment.SettingsFragment;
import com.shalzz.attendance.fragment.TimeTablePagerFragment;
import com.shalzz.attendance.model.UserModel;
import com.shalzz.attendance.wrapper.MyVolley;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

public class MainActivity extends AppCompatActivity {

    /**
     * Reference to fragment positions
     */
    public enum Fragments {
        ATTENDANCE(1),
        TIMETABLE(2),
        SETTINGS(3);

        private final int value;

        Fragments(int value) {
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

    public static final String LAUNCH_FRAGMENT_EXTRA = BuildConfig.APPLICATION_ID +
            ".MainActivity.LAUNCH_FRAGMENT";

    private static final String PREVIOUS_FRAGMENT_TAG = "MainActivity.PREVIOUS_FRAGMENT";

    private static final String mTag = "MainActivity";

    public boolean mPopSettingsBackStack =  false;

    // Views
    @Optional @InjectView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @InjectView(R.id.list_slidermenu) NavigationView mNavigationView;

    private int mContentViewHeight;
    private int mCurrentSelectedPosition = Fragments.ATTENDANCE.getValue();
    private String[] mNavTitles;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerHeaderViewHolder DrawerheaderVH;
    private FragmentManager mFragmentManager;
    private Fragment fragment = null;
    private DatabaseHandler mDb;
    // Our custom poor-man's back stack which has only one entry at maximum.
    private Fragment mPreviousFragment;
    private Toolbar mToolbar;
    private Bundle mSavedInstanceState;
    private ValueAnimator mToolbarHeightAnimator;
    private boolean mResumed = false;
    private boolean initialised = false;

    public static class DrawerHeaderViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.drawer_header_name) TextView tv_name;
        @InjectView(R.id.drawer_header_course) TextView tv_course;
        @InjectView(R.id.last_refreshed) TextView last_refresh;

        public DrawerHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this,itemView);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);
        ButterKnife.inject(this);

        mSavedInstanceState = savedInstanceState;
        isDrawerLocked = getResources().getBoolean(R.bool.tablet_layout);
        mNavTitles = getResources().getStringArray(R.array.drawer_array);
        mFragmentManager = getSupportFragmentManager();
        mDb = new DatabaseHandler(this);
        DrawerheaderVH = new DrawerHeaderViewHolder(mNavigationView.getHeaderView(0));

        /**     ------------- Toolbar init -----------           */

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if(getIntent().hasExtra(SplashActivity.INTENT_EXTRA_STARTING_ACTIVITY)) {
            mToolbar.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mToolbar.getViewTreeObserver().removeOnPreDrawListener(this);
                            final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                            final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

                            mToolbar.measure(widthSpec, heightSpec);
                            mContentViewHeight = mToolbar.getHeight();
                            collapseToolbar();
                            return true;
                        }
                    });
        } else {
            int toolBarHeight;
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            toolBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, getResources().getDisplayMetrics());
            ViewGroup.LayoutParams lp = mToolbar.getLayoutParams();
            lp.height = toolBarHeight;
            mToolbar.setLayoutParams(lp);
        }
        setSupportActionBar(mToolbar);

        /**     ------------- Toolbar init ends -----------           */

        // Set the list's click listener
        mNavigationView.setNavigationItemSelectedListener(new NavigationItemSelectedListener());

        if(!isDrawerLocked)
            initDrawer();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        mResumed = true;
        if(mToolbarHeightAnimator == null || !mToolbarHeightAnimator.isRunning()) {
            init();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        showcaseView();
    }

    /**
     * Initialise a fragment
     **/
    public void init() {
        if(initialised) {
           return;
        }
        initialised = true;

        Log.d(mTag, "init: running!");

        // Select either the default item (Fragments.ATTENDANCE) or the last selected item.
        mCurrentSelectedPosition = reloadCurrentFragment();

        // Recycle fragment
        if(mSavedInstanceState != null) {
            fragment =  mFragmentManager.findFragmentByTag(FRAGMENT_TAG);
            mPreviousFragment = mFragmentManager.getFragment(mSavedInstanceState, PREVIOUS_FRAGMENT_TAG);
            Log.d(mTag, "current fag found: " + fragment );
            Log.d(mTag, "previous fag found: " + mPreviousFragment );
            selectItem(mCurrentSelectedPosition);
            showFragment(fragment);
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
    }

    private void initDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark,
                    getTheme()));
        } else {
            //noinspection deprecation
            mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(
                    R.color.primary_dark));
        }
    }

    private void collapseToolbar() {
        int toolBarHeight;
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        toolBarHeight = TypedValue.complexToDimensionPixelSize(
                tv.data, getResources().getDisplayMetrics());

        mToolbarHeightAnimator = ValueAnimator
                .ofInt(mContentViewHeight, toolBarHeight);

        mToolbarHeightAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        ViewGroup.LayoutParams lp = mToolbar.getLayoutParams();
                        lp.height = (Integer) animation.getAnimatedValue();
                        mToolbar.setLayoutParams(lp);
                    }
                });

        mToolbarHeightAnimator.start();
        mToolbarHeightAnimator.addListener(
                new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if(mResumed)
                            init();
                    }
                });
    }

    void showcaseView() {

        if(isDrawerLocked ) {
            if(fragment instanceof AttendanceListFragment) {
                ((AttendanceListFragment) fragment).showcaseView();
            }
            return;
        }

        Target homeTarget = new Target() {
            @Override
            public Point getPoint() {
                // Get approximate position of home icon's center
                int actionBarSize = mToolbar.getHeight();
                int x = actionBarSize / 2;
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };

        final ShowcaseView sv = new ShowcaseView.Builder(this)
                .setTarget(homeTarget)
                .setStyle(R.style.ShowcaseTheme)
                .singleShot(1111)
                .setContentTitle(getString(R.string.sv_main_activity_title))
                .setContentText(getString(R.string.sv_main_activity_content))
                .build();

        sv.overrideButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDrawerLocked )
                    mDrawerLayout.closeDrawer(mNavigationView);
                sv.hide();
                if(fragment instanceof AttendanceListFragment) {
                    ((AttendanceListFragment) fragment).showcaseView();
                }
            }
        });
    }

    public void updateDrawerHeader() {
        updateUserDetails();
        updateLastSync();
    }

    public void updateUserDetails() {
        if(mDb.getUserCount()>0) {
            UserModel user = mDb.getUser();

            DrawerheaderVH.tv_name.setText(user.getName());
            DrawerheaderVH.tv_course.setText(user.getCourse());
        }
    }

    public void updateLastSync() {
        if(mDb.getRowCount()>0) {
            int time = (int) mDb.getLastSync();
            DrawerheaderVH.last_refresh.setText(
                    getResources().getQuantityString(R.plurals.tv_last_refresh, time, time));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && !isDrawerLocked ) {
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
                mPreviousFragment = null; // GC
                break;
            case 2:
                fragment = new TimeTablePagerFragment();
                mPreviousFragment = null; // GC
                break;
            case 3:
                fragment = new SettingsFragment();
                break;
            default:
                break;
        }

        if (fragment != null) {
            selectItem(position);
            showFragment(fragment);
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
        setTitle(mNavTitles[position-1]);
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
            if (BuildConfig.DEBUG) {
                Log.d(mTag, this + " showFragment: destroying previous fragment "
                        + mPreviousFragment.getClass().getSimpleName());
            }
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
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
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        // close drawer if it is open
        if (!isDrawerLocked && mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        }
        else if (shouldPopFromBackStack()) {
            if(BuildConfig.DEBUG)
                Log.i(mTag,"popping from back stack");
            if(mPopSettingsBackStack) {
                if(BuildConfig.DEBUG)
                    Log.i(mTag,"popping nested settings fragment");
                mPopSettingsBackStack = false;
                mFragmentManager.popBackStackImmediate();
            } else {
                // Custom back stack
                popFromBackStack();
            }
        }
        else {
            ActivityCompat.finishAfterTransition(this);
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
        mToolbar.setTitle(title);
        mToolbar.setSubtitle("");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if(mDrawerToggle != null)
            mDrawerToggle.syncState();
        mSavedInstanceState = savedInstanceState;
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
        mResumed = false;
        // for orientation changes, etc.
        if (mPreviousFragment != null) {
            mFragmentManager.putFragment(outState, PREVIOUS_FRAGMENT_TAG, mPreviousFragment);
            Log.d(mTag, "previous fag saved: " + mPreviousFragment.getClass().getSimpleName());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause() {
        persistCurrentFragment();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if(!isDrawerLocked)
            mDrawerLayout.removeDrawerListener(mDrawerToggle);
        MyVolley.getInstance().cancelAllPendingRequests();
        mDb.close();
        super.onDestroy();
    }

}