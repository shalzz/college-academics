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

package com.shalzz.attendance.fragment;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.CircularIndeterminate;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.TimeTablePagerAdapter;
import com.shalzz.attendance.controllers.PagerController;
import com.shalzz.attendance.controllers.UserAccount;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MyVolley;

import java.util.Calendar;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TimeTablePagerFragment extends Fragment {

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @InjectView(R.id.swiperefresh)
    public MultiSwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.circular_indet)
    public CircularIndeterminate mProgress;
    @InjectView(R.id.pager)
    public ViewPager mViewPager;

    private DrawerLayout mDrawerLayout;
    private NavigationView mDrawerList;

    /**
     * Remember the position of the previous pager position.
     */
    private static final String STATE_PREVIOUS_POSITION = "previous_pager_position";

    private int mPreviousPosition = 15;
    private PagerController mController;
    private TimeTablePagerAdapter mTimeTablePagerAdapter;
    private String myTag = "Pager Fragment";
    private Context mContext;
    private ActionBar actionbar;
    private Date mToday;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(container==null)
            return null;

        if (savedInstanceState != null) {
            mPreviousPosition = savedInstanceState.getInt(STATE_PREVIOUS_POSITION);
        }

        setHasOptionsMenu(true);
        setRetainInstance(false);
        actionbar= ((AppCompatActivity)getActivity()).getSupportActionBar();
        final View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        ButterKnife.inject(this,view);

        mSwipeRefreshLayout.setSwipeableChildren(R.id.pager);
        mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        mDrawerList = (NavigationView) getActivity().findViewById(R.id.list_slidermenu);

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                mPreviousPosition = position;
                updateTitle();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToday =  DateHelper.getToDay();
        mTimeTablePagerAdapter = new TimeTablePagerAdapter(getActivity().getFragmentManager(), mToday);
        mViewPager.setAdapter(mTimeTablePagerAdapter);

        mController = new PagerController(mContext, this);
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getTimetableCount()<=0) {
            mController.updatePeriods();
            mProgress.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else
            mViewPager.setCurrentItem(mPreviousPosition, true);

        if(MyPreferencesManager.isFirstLaunch(myTag))
            showcaseView();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mController.updatePeriods();
            }
        });

        // fix for oversensitive horizontal scroll of swipe view
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setEnabled(false);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            mSwipeRefreshLayout.setEnabled(true);
                            break;
                    }
                }
                return false;
            }
        });
    }

    public void showcaseView() {
        final ShowcaseView sv = new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .doNotBlockTouches()
                .setContentTitle(getString(R.string.sv_timetable_title))
                .setContentText(getString(R.string.sv_timetable_content))
                .build();
        MyPreferencesManager.setFirstLaunch(myTag);

        sv.overrideButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sv.hide();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.time_table, menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.menu_refresh).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_date).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_today).setVisible(!drawerOpen);

        updateTitle();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_logout) {
            new UserAccount(mContext).Logout();
        }
        else if(item.getItemId() == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(true);
                mController.updatePeriods();
            }
        }
        else if(item.getItemId() == R.id.menu_date) {
            Calendar today = Calendar.getInstance();
            today.setTime(mToday);
            DatePickerDialog mDatePickerDialog = new DatePickerDialog(mContext,onDateSetListner()
                    ,today.get(Calendar.YEAR)
                    ,today.get(Calendar.MONTH)
                    ,today.get(Calendar.DAY_OF_MONTH));
            mDatePickerDialog.show();
        }
        else if(item.getItemId() == R.id.menu_today) {
            if(mTimeTablePagerAdapter.getDate() != mToday) {
                mTimeTablePagerAdapter.setDate(mToday);
            }
            scrollToToday();
            updateTitle();
        }
        return super.onOptionsItemSelected(item);
    }
    public void clearFragmentsData() {
        for (DayFragment fragment : mTimeTablePagerAdapter.getActiveFragments()) {
            fragment.clear();
        }
    }

    public void updateFragmentsData() {
        for (DayFragment fragment : mTimeTablePagerAdapter.getActiveFragments()) {
//            Log.d("TimeTableActivity", "Update Fragment " + fragment.getDate() + " with new data.");
            fragment.update();
        }
        updateTitle();
    }

    private void updateTitle() {
        DayFragment fragment = mTimeTablePagerAdapter.getFragment(mPreviousPosition);
//        Log.d(myTag,"Dayfragment: " + fragment);
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        if(drawerOpen) {
            String mNavTitles[] = getResources().getStringArray(R.array.drawer_array);
            actionbar.setTitle(mNavTitles[MainActivity.Fragments.TIMETABLE.getValue()-1]);
            actionbar.setSubtitle("");
        }
        else if(fragment!=null) {
            Date mDate = fragment.getDate();
            actionbar.setTitle(DateHelper.getProperWeekday(mDate));
            actionbar.setSubtitle(DateHelper.formatToProperFormat(mDate));
        }
    }

    private void scrollToToday() {
        mViewPager.setCurrentItem(15, true);
    }

    DatePickerDialog.OnDateSetListener onDateSetListner() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);
                mTimeTablePagerAdapter.setDate(date.getTime());
                updateTitle();
                scrollToToday();
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

}
