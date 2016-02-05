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
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

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
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyVolley;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AttendanceListFragment extends Fragment implements
        ExpandableListAdapter.SubjectItemExpandedListener {

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @InjectView(R.id.swipe_refresh_atten)
    public MultiSwipeRefreshLayout mSwipeRefreshLayout;

    @InjectView(R.id.circular_indet_atten)
    public CircularIndeterminate mProgress;

    @InjectView(R.id.atten_recycler_view)
    public RecyclerView mRecyclerView;

    private boolean useGridLayout = false;

    private final int GRID_LAYOUT_SPAN_COUNT = 2;

    private LinearLayoutManager mLinearLayoutManager;
    private StaggeredGridLayoutManager mGridLayoutManager;
    private Context mContext;
    private String mTag = "Attendance List Fragment";
    private Resources mResources;
    private AttendanceController controller;

    private float mExpandedItemTranslationZ;
    private int mFadeInDuration = 150;
    private int mFadeInStartDelay = 150;
    private int mFadeOutDuration = 20;
    private int mExpandCollapseDuration = 200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	Bugsnag.setContext("AttendanceList");
        mContext = getActivity();
        mResources = getResources();
        mExpandedItemTranslationZ =
                mResources.getDimension(R.dimen.atten_view_expanded_elevation);
        useGridLayout = mResources.getBoolean(R.bool.use_grid_layout);
    }

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        if(container==null)
            return null;

        setHasOptionsMenu(true);
        View mView = inflater.inflate(R.layout.fragment_attendance, container, false);
        ButterKnife.inject(this, mView);

        mSwipeRefreshLayout.setSwipeableChildren(R.id.atten_recycler_view);

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);

        mLinearLayoutManager = new LinearLayoutManager(mContext,
                LinearLayoutManager.VERTICAL, false);
        mLinearLayoutManager.setSmoothScrollbarEnabled(true);
        mGridLayoutManager = new StaggeredGridLayoutManager(GRID_LAYOUT_SPAN_COUNT,
                StaggeredGridLayoutManager.VERTICAL);
        mGridLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);

        mRecyclerView.setLayoutManager(useGridLayout ? mGridLayoutManager : mLinearLayoutManager);

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST);
        mRecyclerView.addItemDecoration(itemDecoration);

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        controller = new AttendanceController(mContext, this);

        if(!controller.hasSubjects()) {
            controller.updateSubjects();
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                controller.updateSubjects();
            }
        });

    }

    public void showcaseView() {
        View firstElementView = mRecyclerView.getChildAt(0);
        ViewTarget target = firstElementView != null ? new ViewTarget(firstElementView)
                : new ViewTarget(mRecyclerView);

        new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(target)
                .setContentTitle(getString(R.string.sv_attendance_title))
                .setContentText(getString(R.string.sv_attendance_content))
                .build();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(mResources.getString(R.string.hint_search));

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                controller.getSubjects();
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                return true;  // Return true to expand action view
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String arg0) {
                Miscellaneous.closeKeyboard(mContext, searchView);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String arg0) {
                controller.getSubjectsLike(arg0);
                return false;
            }
        });
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
                controller.updateSubjects();
            }
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
                // We don't want to continue getting called for every draw.
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

                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        Float value = (Float) animator.getAnimatedValue();

                        // For each value from 0 to 1, animate the various parts of the layout.
                        view.getLayoutParams().height = (int) (value * distance + baseHeight);
                        float z = mExpandedItemTranslationZ * value;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            view.setTranslationZ(z);
                        }
                        view.requestLayout();
                    }
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
        MyVolley.getInstance().cancelPendingRequests(MyVolley.ACTIVITY_NETWORK_TAG);
        ButterKnife.reset(this);
    }
}
