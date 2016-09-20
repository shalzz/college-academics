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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.CircularIndeterminate;
import com.shalzz.attendance.DividerItemDecoration;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.adapter.ExpandableListAdapter;
import com.shalzz.attendance.controllers.AttendanceController;
import com.shalzz.attendance.controllers.UserAccount;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyApplication;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindBool;
import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class AttendanceListFragment extends Fragment implements
        ExpandableListAdapter.SubjectItemExpandedListener {

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

    @BindBool(R.bool.use_grid_layout)
    boolean useGridLayout;

    @BindDimen(R.dimen.atten_view_expanded_elevation)
    float mExpandedItemTranslationZ;

    @BindString(R.string.hint_search)
    String hint_search_view;

    @Inject @Named("app")
    Tracker mTracker;

    @Inject
    DataAPI api;

    UserAccount userAccount;

    @Nullable
    private LinearLayoutManager mLinearLayoutManager;

    @Nullable
    private StaggeredGridLayoutManager mGridLayoutManager;

    private Context mContext;
    private String mTag = "Attendance List Fragment";
    private AttendanceController controller;

    private final int GRID_LAYOUT_SPAN_COUNT = 2;
    private final int mFadeInDuration = 150;
    private final int mFadeInStartDelay = 150;
    private final int mFadeOutDuration = 20;
    private final int mExpandCollapseDuration = 200;
    private Unbinder unbinder;
    public EmptyView mEmptyView = new EmptyView();

    @Override
    public void onStart() {
        super.onStart();

        mTracker.setScreenName(getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        if(container==null)
            return null;
	    Bugsnag.setContext("AttendanceList");
        MyApplication.getAppComponent().inject(this);

        mContext = getActivity();
        userAccount = new UserAccount(mContext,api);
        setHasOptionsMenu(true);
        View mView = inflater.inflate(R.layout.fragment_attendance, container, false);
        unbinder = ButterKnife.bind(this, mView);
        ButterKnife.bind(mEmptyView, emptyView);

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

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        controller = new AttendanceController(mContext, this, api);
        mProgress.setVisibility(View.VISIBLE);

        mSwipeRefreshLayout.setOnRefreshListener(() -> controller.updateSubjects());

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, controller);
    }

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
                Bundle bundle = new Bundle();
                bundle.putString(AttendanceController.SUBJECT_FILTER,filter);
                // destroy the loader first to clear the adapter
                getLoaderManager().destroyLoader(0);
                getLoaderManager().restartLoader(0, bundle, controller);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_logout) {
            userAccount.Logout();
            return true;
        }
        else if(item.getItemId() == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(true);
                controller.updateSubjects();
            }
            return true;
        }
        else if(item.getItemId() == R.id.menu_search) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Search")
                    .build());
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

                // Set up the fade effect for the action buttons.
                if (isExpanded) {
                    // Start the fade in after the expansion has partly completed, otherwise it
                    // will be mostly over before the expansion completes.
                    viewHolder.childView.setAlpha(0f);
                    viewHolder.childView.animate()
                            .alpha(1f)
                            .setStartDelay(mFadeInStartDelay)
                            .setDuration(mFadeInDuration)
                            .start();
                } else {
                    viewHolder.childView.setAlpha(1f);
                    viewHolder.childView.animate()
                            .alpha(0f)
                            .setDuration(mFadeOutDuration)
                            .start();
                }
                view.requestLayout();

                // Set up the animator to animate the expansion and shadow depth.
                ValueAnimator animator = isExpanded ? ValueAnimator.ofFloat(0f, 1f)
                        : ValueAnimator.ofFloat(1f, 0f);

                // scroll to make the view fully visible.
                mRecyclerView.smoothScrollToPosition(viewHolder.position);

                animator.addUpdateListener(animator1 -> {
                    Float value = (Float) animator1.getAnimatedValue();

                    // For each value from 0 to 1, animate the various parts of the layout.
                    view.getLayoutParams().height = (int) (value * distance + baseHeight);
                    float z = mExpandedItemTranslationZ * value;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        view.setTranslationZ(z);
                    }
                    view.requestLayout();
                });

                // Set everything to their final values when the animation's done.
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

                        if (!isExpanded) {
                            viewHolder.childView.setVisibility(View.GONE);
                        } else {
                            // This seems like it should be unnecessary, but without this, after
                            // navigating out of the activity and then back, the action view alpha
                            // is defaulting to the value (0) at the start of the expand animation.
                            viewHolder.childView.setAlpha(1);
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
        controller.destroyAds();
    }
}
