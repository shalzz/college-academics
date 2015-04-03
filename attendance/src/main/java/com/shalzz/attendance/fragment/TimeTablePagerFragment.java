/*
 * Copyright (c) 2014 Shaleen Jain <shaleen.jain95@gmail.com>
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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.shalzz.attendance.CircularIndeterminate;
import com.shalzz.attendance.DataAPI;
import com.shalzz.attendance.DataAssembler;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.UserAccount;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.TimeTablePagerAdapter;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.ErrorHelper;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import java.util.Calendar;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TimeTablePagerFragment extends Fragment {

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @InjectView(R.id.swiperefresh) MultiSwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.circular_indet) CircularIndeterminate mProgress;
    @InjectView(R.id.pager) ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    /**
     * Remember the position of the previous pager position.
     */
    private static final String STATE_PREVIOUSE_POSITION = "previous_pager_position";

    private int mPreviousPosition = 15;
    private TimeTablePagerAdapter mTimeTablePagerAdapter;
    private String myTag = "Pager Fragment";
    private Context mContext;
    private ActionBar actionbar;
    private Date mToday;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        Bugsnag.setContext("Timetable");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(container==null)
            return null;

        if (savedInstanceState != null) {
            mPreviousPosition = savedInstanceState.getInt(STATE_PREVIOUSE_POSITION);
        }

        setHasOptionsMenu(true);
        setRetainInstance(false);
        actionbar= ((ActionBarActivity)getActivity()).getSupportActionBar();
        final View view = inflater.inflate(R.layout.swipe_layout, container, false);
        ButterKnife.inject(this,view);

        mSwipeRefreshLayout.setSwipeableChildren(R.id.pager);
        mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) getActivity().findViewById(R.id.list_slidermenu);

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        mToday =  DateHelper.getToDay();
        mTimeTablePagerAdapter = new TimeTablePagerAdapter(getActivity().getFragmentManager(), mToday);
        mViewPager.setAdapter(mTimeTablePagerAdapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            public void onPageScrollStateChanged(int state) {}

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

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
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getRowCountofTimeTable()<=0) {
            DataAPI.getTimeTable(mContext, timeTableSuccessListener(), myErrorListener());
            mProgress.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else
            mViewPager.setCurrentItem(mPreviousPosition, true);

        MyPreferencesManager prefs = new MyPreferencesManager(mContext);
        if(prefs.isFirstLaunch(myTag))
            showcaseView();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                DataAPI.getTimeTable(mContext, timeTableSuccessListener(), myErrorListener());
            }
        });

        // fix for oversensitive horizontal scroll of swipe view
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mSwipeRefreshLayout != null) {
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
        MyPreferencesManager prefs = new MyPreferencesManager(mContext);
        final ShowcaseView sv = new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .doNotBlockTouches()
                .setContentTitle(getString(R.string.sv_timetable_title))
                .setContentText(getString(R.string.sv_timetable_content))
                .build();
        prefs.setFirstLaunch(myTag);

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
                DataAPI.getTimeTable(mContext, timeTableSuccessListener(), myErrorListener());
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

    public void updateFragments() {
        for (DayFragment fragment : mTimeTablePagerAdapter.getActiveFragments()) {
//            Log.d("TimeTableActivity", "Update Fragment " + fragment.getDate() + " with new data.");
            fragment.reloadDataSet();
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

    private Response.Listener<String> timeTableSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    new DataAssembler.ParseTimeTable(mContext, parseListener()).execute(response);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Bugsnag.notify(e, Severity.ERROR);
                    String msg = getResources().getString(R.string.unexpected_error);
                    Miscellaneous.showSnackBar(mContext, msg);
                }
            }
        };
    }

    private Response.ErrorListener myErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Stop the refreshing indicator
                if(mProgress == null || mSwipeRefreshLayout == null)
                    return;
                mProgress.setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setRefreshing(false);
                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                Miscellaneous.showSnackBar(mContext, msg);
                Log.e(myTag, msg);
            }
        };
    }

    private DataAssembler.Listener parseListener() {
        return new DataAssembler.Listener() {
            @Override
            public void onParseComplete(int result) {
                // Stop the refreshing indicator
                if(mProgress == null || mSwipeRefreshLayout == null)
                    return;
                mProgress.setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setRefreshing(false);
                if(result == 0) {
                    mTimeTablePagerAdapter.setDate(mToday);
                    updateFragments();
                    scrollToToday();
                    updateTitle();
                }
                ErrorHelper.handleError(result, mContext);
            }
        };
    }

    public void notifyDataSetChanged() {
        mTimeTablePagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PREVIOUSE_POSITION, mPreviousPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

}
