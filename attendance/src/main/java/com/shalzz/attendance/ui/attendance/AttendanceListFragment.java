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

package com.shalzz.attendance.ui.attendance;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bugsnag.android.Bugsnag;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.malinskiy.materialicons.IconDrawable;
import com.malinskiy.materialicons.Iconify;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.ui.login.UserAccount;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.CircularIndeterminate;
import com.shalzz.attendance.utils.DividerItemDecoration;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindBool;
import butterknife.BindInt;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class AttendanceListFragment extends Fragment implements
        AttendanceMvpView,
        ExpandableListAdapter.SubjectItemExpandedListener {

    private final int GRID_LAYOUT_SPAN_COUNT = 2;

    @Inject
    UserAccount mUserAccount;

    @Inject
    AttendancePresenter mPresenter;

    @Inject
    ExpandableListAdapter mAdapter;

    static class EmptyView {
        @BindView(R.id.emptyStateImageView)
        ImageView ImageView;

        @BindView(R.id.emptyStateTitleTextView)
        TextView TitleTextView;

        @BindView(R.id.emptyStateContentTextView)
        TextView ContentTextView;

        @BindView(R.id.emptyStateButton)
        Button Button;
    }

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @BindView(R.id.swipe_refresh_atten)
    public MultiSwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.circular_indet_atten)
    public CircularIndeterminate mProgress;

    @BindView(R.id.atten_recycler_view)
    public RecyclerView mRecyclerView;

    @BindView(R.id.empty_view)
    public View emptyView;

    @BindBool(R.bool.use_grid_layout)
    boolean useGridLayout;

    @BindInt(R.integer.expand_collapse_duration)
    int mExpandCollapseDuration;

    @BindString(R.string.hint_search)
    String hint_search_view;

    @Nullable private LinearLayoutManager mLinearLayoutManager;
    @Nullable private StaggeredGridLayoutManager mGridLayoutManager;
    private Context mContext;
    private Unbinder unbinder;
    public EmptyView mEmptyView = new EmptyView();

    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.fragment_attendance, container, false);
        unbinder = ButterKnife.bind(this, mView);
        ButterKnife.bind(mEmptyView, emptyView);

        ((MainActivity) getActivity()).activityComponent().inject(this);
        Bugsnag.setContext("AttendanceList");
        mPresenter.attachView(this);

        mContext = getActivity();
        setHasOptionsMenu(true);

        mSwipeRefreshLayout.setOnRefreshListener(() -> mPresenter.syncSubjects());
        mSwipeRefreshLayout.setSwipeableChildren(R.id.atten_recycler_view);

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        if(useGridLayout) {
            mGridLayoutManager = new StaggeredGridLayoutManager(GRID_LAYOUT_SPAN_COUNT,
                    StaggeredGridLayoutManager.VERTICAL);
            mGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        } else {
            mLinearLayoutManager = new LinearLayoutManager(mContext,
                    LinearLayoutManager.VERTICAL, false);
            mLinearLayoutManager.setSmoothScrollbarEnabled(false);
            mLinearLayoutManager.setStackFromEnd(false);
            mLinearLayoutManager.setAutoMeasureEnabled(true);
        }

        mRecyclerView.setLayoutManager(useGridLayout ? mGridLayoutManager : mLinearLayoutManager);

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        View mFooter = inflater.inflate(R.layout.list_footer, mRecyclerView, false);
        mFooter.setVisibility(View.INVISIBLE);
        mAdapter.addFooter(mFooter);
        mAdapter.setCallback(this);

        mRecyclerView.setAdapter(mAdapter);

        mProgress.setVisibility(View.VISIBLE);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.loadSubjects(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(hint_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String arg0) {
                Miscellaneous.closeKeyboard(mContext, searchView);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String arg0) {
                String filter = !TextUtils.isEmpty(arg0) ? arg0 : null;
                clearSubjects();
                mPresenter.loadSubjects(filter);
                return false;
            }
        });
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
                mPresenter.syncSubjects();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemExpanded(final View view) {
        final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final ExpandableListAdapter.GenericViewHolder viewHolder = (ExpandableListAdapter.GenericViewHolder) view.getTag();
        final RelativeLayout childView = viewHolder.childView;
        childView.measure(spec, spec);
        final int startingHeight = view.getHeight();
        final ViewTreeObserver observer = mRecyclerView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // We don'mTracker want to continue getting called for every draw.
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                // Calculate some values to help with the animation.
                final int endingHeight = view.getHeight();
                final int distance = Math.abs(endingHeight - startingHeight);
                final int baseHeight = Math.min(endingHeight, startingHeight);
                final boolean isExpanded = endingHeight > startingHeight;

                // Set the views back to the start state of the animation
                view.getLayoutParams().height = startingHeight;
                if (!isExpanded) {
                    viewHolder.childView.setVisibility(View.VISIBLE);
                }

                // Set up the animator to animate the expansion and shadow depth.
                ValueAnimator animator = isExpanded ? ValueAnimator.ofFloat(0f, 1f)
                        : ValueAnimator.ofFloat(1f, 0f);

                // scroll to make the view fully visible.
                mRecyclerView.smoothScrollToPosition(viewHolder.position);

                animator.addUpdateListener(animator1 -> {
                    Float value = (Float) animator1.getAnimatedValue();

                    // For each value from 0 to 1, animate the various parts of the layout.
                    view.getLayoutParams().height = (int) (value * distance + baseHeight);
                    view.requestLayout();
                });

                // Set everything to their final values when the animation's done.
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

                        if (!isExpanded) {
                            viewHolder.childView.setVisibility(View.GONE);
                        }
                    }
                });

                animator.setDuration(mExpandCollapseDuration);
                animator.start();

                // Return false so this draw does not occur to prevent the final frame from
                // being drawn for the single frame before the animations start.
                return false;
            }
        });
    }

    @Override
    public View getViewForCallId(long callId) {
        if(!useGridLayout) {
            assert mLinearLayoutManager != null;
            int firstPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
            int lastPosition = mLinearLayoutManager.findLastVisibleItemPosition();

            for (int position = 0; position <= lastPosition - firstPosition; position++) {
                View view = mRecyclerView.getChildAt(position);

                if (view != null) {
                    final ExpandableListAdapter.GenericViewHolder viewHolder =
                            (ExpandableListAdapter.GenericViewHolder) view.getTag();
                    if (viewHolder != null && viewHolder.position == callId) {
                        return view;
                    }
                }
            }
        } else {
            int firstPosition[] = {0, 0};
            int lastPosition[] = {0, 0} ;
            assert mGridLayoutManager != null;
            mGridLayoutManager.findFirstVisibleItemPositions(firstPosition);
            mGridLayoutManager.findLastVisibleItemPositions(lastPosition);

            for (int i = 0 ; i< GRID_LAYOUT_SPAN_COUNT; i++) {
                for (int position = 0; position <= lastPosition[i] - firstPosition[i] ; position++) {
                    View view = mRecyclerView.getChildAt(position);

                    if (view != null) {
                        final ExpandableListAdapter.GenericViewHolder viewHolder =
                                (ExpandableListAdapter.GenericViewHolder) view.getTag();
                        if (viewHolder != null && viewHolder.position == callId) {
                            return view;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        mPresenter.detachView();
    }

    /***** MVP View methods implementation *****/

    @Override
    public void clearSubjects() {
        mAdapter.clear();
    }

    @Override
    public void addSubjects(List<Subject> subjects) {
        stopRefreshing();
        showEmptyView(false);
        mAdapter.addAll(subjects);
    }

    @Override
    public void showcaseView() {
        if(mRecyclerView != null && mRecyclerView.getChildAt(2) != null) {
            ViewTarget target = new ViewTarget(mRecyclerView.getChildAt(2));

            new ShowcaseView.Builder(getActivity())
                    .setStyle(R.style.ShowcaseTheme)
                    .setTarget(target)
                    .singleShot(2222)
                    .setContentTitle(getString(R.string.sv_attendance_title))
                    .setContentText(getString(R.string.sv_attendance_content))
                    .build();
        }
    }

    @Override
    public void updateLastSync() {
        ((MainActivity) getActivity()).updateLastSync();
    }

    @Override
    public void stopRefreshing() {
        mProgress.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showError(String message) {
        stopRefreshing();
        Miscellaneous.showSnackBar(mRecyclerView, message);
    }

    @Override
    public void showRetryError(String message) {
        stopRefreshing();
        Snackbar.make(mRecyclerView, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> mPresenter.syncSubjects())
                .show();
    }

    @Override
    public void showEmptyView(boolean show) {
        stopRefreshing();
        if(show) {
            emptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showNetworkErrorView() {
        Drawable emptyDrawable = new IconDrawable(mContext,
                Iconify.IconValue.zmdi_wifi_off)
                .colorRes(android.R.color.darker_gray);
        mEmptyView.ImageView.setImageDrawable(emptyDrawable);
        mEmptyView.TitleTextView.setText(R.string.no_connection_title);
        mEmptyView.ContentTextView.setText(R.string.no_connection_content);
        mEmptyView.Button.setOnClickListener( v -> mPresenter.syncSubjects());
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

        updateLastSync();
        showEmptyView(true);
    }
}
