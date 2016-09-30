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

package com.shalzz.attendance.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bugsnag.android.Bugsnag;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.ui.login.UserAccount;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.CircularIndeterminate;
import com.shalzz.attendance.wrapper.DateHelper;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

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

    @BindView(R.id.empty_view)
    public View emptyView;

    public static class EmptyView {
        @BindView(R.id.emptyStateImageView)
        public ImageView ImageView;

        @BindView(R.id.emptyStateTitleTextView)
        public TextView TitleTextView;

        @BindView(R.id.emptyStateContentTextView)
        public TextView ContentTextView;

        @BindView(R.id.emptyStateButton)
        public Button Button;
    }

    @Inject
    DataAPI api;

    @Inject
    UserAccount mUserAccount;

    private int mPreviousPosition = 15;
    private PagerController mController;
    private Context mContext;
    private ActionBar actionbar;
    private Unbinder unbinder;
    public EmptyView mEmptyView = new EmptyView();

    @Override
    public void onStart() {
        super.onStart();
        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getPeriodCount() == 0) {
            mController.updatePeriods();
            mProgress.setVisibility(View.VISIBLE);
            mViewPager.setVisibility(View.GONE);
        }
        else
            mController.setToday();
        db.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(container==null)
            return null;
        mContext = getActivity();
        Bugsnag.setContext("Timetable");
        ((MainActivity) getActivity()).activityComponent().inject(this);

        setHasOptionsMenu(true);
        setRetainInstance(false);
        actionbar= ((AppCompatActivity)getActivity()).getSupportActionBar();
        final View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        unbinder = ButterKnife.bind(this, view);
        ButterKnife.bind(mEmptyView, emptyView);

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

        mController = new PagerController(mContext, this, getActivity().getSupportFragmentManager
                (),api);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mController.updatePeriods());

        // fix for oversensitive horizontal scroll of swipe view
        mViewPager.setOnTouchListener((v, event) -> {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setEnabled(false);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        mSwipeRefreshLayout.setEnabled(true);
                        break;
                }
            }
            return false;
        });

    }

    @Override
    public void onResume() {
        super.onResume();
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

        sv.overrideButtonClick(v -> sv.hide());
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
            mUserAccount.Logout();
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
        return (view, year, monthOfYear, dayOfMonth) -> {
            Calendar date = Calendar.getInstance();
            date.set(year, monthOfYear, dayOfMonth);
            mController.setDate(date.getTime());
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
