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

package com.shalzz.attendance.fragment;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
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
import com.shalzz.attendance.controllers.PagerController;
import com.shalzz.attendance.controllers.UserAccount;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyVolley;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

public class TimeTablePagerFragment extends Fragment {

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @BindView(R.id.swiperefresh)
    public MultiSwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.circular_indet)
    public CircularIndeterminate mProgress;

    @BindView(R.id.pager)
    public ViewPager mViewPager;

    private int mPreviousPosition = 15;
    private PagerController mController;
    private String myTag = "Pager Fragment";
    private Context mContext;
    private ActionBar actionbar;
    private Unbinder unbinder;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.setScreenName(getClass().getSimpleName());
        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(container==null)
            return null;

        setHasOptionsMenu(true);
        setRetainInstance(false);
        actionbar= ((AppCompatActivity)getActivity()).getSupportActionBar();
        final View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        unbinder = ButterKnife.bind(this, view);

        mSwipeRefreshLayout.setSwipeableChildren(R.id.time_table_recycler_view);

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        mViewPager.setOffscreenPageLimit(3);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mController = new PagerController(mContext, this, getActivity().getSupportFragmentManager());
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getPeriodCount()<=0) {
            mController.updatePeriods();
            mProgress.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else
            mController.setToday();
        db.close();

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

        showcaseView();
    }

    public void showcaseView() {
        final ShowcaseView sv = new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .singleShot(3333)
                .doNotBlockTouches()
                .setContentTitle(getString(R.string.sv_timetable_title))
                .setContentText(getString(R.string.sv_timetable_content))
                .build();

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
        updateTitle(-1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_logout) {
            new UserAccount(mContext).Logout();
            return true;
        }
        else if(item.getItemId() == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(true);
                mController.updatePeriods();
                return true;
            }
        }
        else if(item.getItemId() == R.id.menu_date) {
            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            DatePickerDialog mDatePickerDialog = new DatePickerDialog(mContext, onDateSetListener()
                    ,today.get(Calendar.YEAR)
                    ,today.get(Calendar.MONTH)
                    ,today.get(Calendar.DAY_OF_MONTH));
            mDatePickerDialog.show();
            return true;
        }
        else if(item.getItemId() == R.id.menu_today) {
            mController.setToday();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update action bar title and subtitle
     * @param position to update for, -1 for current page
     */
    @OnPageChange(R.id.pager)
    public void updateTitle(int position) {
        if(position > 0)
            mPreviousPosition = position;
        Date mDate  = mController.getDateForPosition(mPreviousPosition);
        if(mDate!=null) {
            actionbar.setTitle(DateHelper.getProperWeekday(mDate));
            actionbar.setSubtitle(DateHelper.formatToProperFormat(mDate));
        }
    }

    DatePickerDialog.OnDateSetListener onDateSetListener() {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);
                mController.setDate(date.getTime());
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MyVolley.getInstance().cancelPendingRequests(MyVolley.ACTIVITY_NETWORK_TAG);
        unbinder.unbind();
    }
}
