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

package com.shalzz.attendance.ui.day;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.injection.ActivityContext;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.CircularIndeterminate;
import com.shalzz.attendance.utils.DividerItemDecoration;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class DayFragment extends Fragment implements DayMvpView {

    public static final String ARG_DATE = "date";

    @Inject
    DayPresenter mDayPresenter;

    @Inject
    DayListAdapter mAdapter;

    @Inject
    @ActivityContext
    Context mContext;

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @BindView(R.id.swiperefresh)
    public MultiSwipeRefreshLayout mSwipeRefreshLayout;

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

    @BindView(R.id.time_table_recycler_view)
    public RecyclerView mRecyclerView;

    private Date mDate;
    private EmptyView mEmptyView = new EmptyView();
    private Unbinder unbinder;

    /**
     * Create a new instance of DayFragment, providing "Date"
     * as an argument.
     */
    public static DayFragment newInstance(Date date) {
        DayFragment f = new DayFragment();

        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_day, container, false);
        unbinder = ButterKnife.bind(this, mView);
        ButterKnife.bind(mEmptyView, emptyView);

        ((MainActivity) getActivity()).activityComponent().inject(this);
        mDayPresenter.attachView(this);

        mDate = getArguments().getSerializable(ARG_DATE) != null ?
                (Date) getArguments().getSerializable(ARG_DATE) : new Date();

        mSwipeRefreshLayout.setSwipeableChildren(R.id.time_table_recycler_view);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);
        mSwipeRefreshLayout.setOnRefreshListener(() -> mDayPresenter.syncDay(mDate));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        mRecyclerView.setAdapter(mAdapter);

        mDayPresenter.getDay(mDate);

        return mView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(true);
                mDayPresenter.syncDay(mDate);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(unbinder!=null)
            unbinder.unbind();
        mDayPresenter.detachView();
    }

    /***** MVP View methods implementation *****/

    @Override
    public void setRefreshing() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
    }

    @Override
    public void stopRefreshing() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void clearDay() {
        showNoTimetableEmptyView();
        mAdapter.clear();
    }

    @Override
    public void setDay(List<Period> day) {
        showEmptyView(false);
        mAdapter.update(day);
    }

    @Override
    public void showEmptyView(boolean show) {
        stopRefreshing();
        if(show) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void showNoTimetableEmptyView() {
        mEmptyView.ImageView.setVisibility(View.GONE);
        mEmptyView.TitleTextView.setText(R.string.no_classes);
        mEmptyView.ContentTextView.setText(R.string.swipe_info);
        mEmptyView.Button.setVisibility(View.GONE);

        showEmptyView(true);
    }

    @Override
    public void showNoConnectionErrorView() {
        mEmptyView.ImageView.setVisibility(View.VISIBLE);
        Drawable emptyDrawable = new IconDrawable(mContext,
                Iconify.IconValue.zmdi_wifi_off)
                .colorRes(android.R.color.darker_gray);
        mEmptyView.ImageView.setImageDrawable(emptyDrawable);
        mEmptyView.TitleTextView.setText(R.string.no_connection_title);
        mEmptyView.ContentTextView.setText(R.string.no_connection_content);
        mEmptyView.Button.setOnClickListener( v -> mDayPresenter.syncDay(mDate));
        mEmptyView.Button.setVisibility(View.VISIBLE);

        showEmptyView(true);
    }

    @Override
    public void showNetworkErrorView(String error) {
        Drawable emptyDrawable = new IconDrawable(mContext,
                Iconify.IconValue.zmdi_network_alert)
                .colorRes(android.R.color.darker_gray);
        mEmptyView.ImageView.setImageDrawable(emptyDrawable);
        mEmptyView.TitleTextView.setText(R.string.network_error_message);
        mEmptyView.ContentTextView.setText(error);
        mEmptyView.Button.setOnClickListener( v -> mDayPresenter.syncDay(mDate));
        mEmptyView.Button.setVisibility(View.VISIBLE);

        showEmptyView(true);
    }

    @Override
    public void showEmptyErrorView() {
        Drawable emptyDrawable = new IconDrawable(mContext,
                Iconify.IconValue.zmdi_cloud_off)
                .colorRes(android.R.color.darker_gray);
        mEmptyView.ImageView.setImageDrawable(emptyDrawable);
        mEmptyView.TitleTextView.setText(R.string.no_data_title);
        mEmptyView.ContentTextView.setText(R.string.no_data_content);
        mEmptyView.Button.setVisibility(View.GONE);

        showEmptyView(true);
    }

    @Override
    public void showError(String message) {
        Timber.d("Error: %s", message);
        stopRefreshing();
        Miscellaneous.showSnackBar(mSwipeRefreshLayout, message);
    }
}
