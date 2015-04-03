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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.shalzz.attendance.CircularIndeterminate;
import com.shalzz.attendance.DataAPI;
import com.shalzz.attendance.DataAssembler;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.UserAccount;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.adapter.ExpandableListAdapter;
import com.shalzz.attendance.model.ListFooter;
import com.shalzz.attendance.model.ListHeader;
import com.shalzz.attendance.model.Subject;
import com.shalzz.attendance.wrapper.ErrorHelper;
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MySyncManager;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AttendanceListFragment extends ListFragment implements 
        ExpandableListAdapter.SubjectItemExpandedListener {

    /**
     * The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    @InjectView(R.id.swipe_refresh_atten)
    MultiSwipeRefreshLayout mSwipeRefreshLayout;

    @InjectView(R.id.circular_indet_atten)
    CircularIndeterminate mProgress;

    private View mFooter;
    private View mHeader;
    private HeaderFooterViewHolder mHFViewHolder;
    private ListView mListView;
    private TextView mLastRefreshView;
    private Context mContext;
    private String mTag;
    private ExpandableListAdapter mAdapter;
    private MyPreferencesManager prefs;
    private View mDropShadow;
    private Resources mResourses;

    private float mExpandedItemTranslationZ;
    private int mFadeInDuration = 150;
    private int mFadeInStartDelay = 150;
    private int mFadeOutDuration = 20;
    private int mExpandCollapseDuration = 200;

    /**
     * View Holder for list view header and footer views.
     */
    protected static class HeaderFooterViewHolder {

        public TextView tvName;
        public TextView tvSap;
        public TextView tvCourse;
        public TextView tvPercent;
        public TextView tvClasses;
        public ProgressBar pbPercent;

        public HeaderFooterViewHolder (View header, View footer) {

            tvName = (TextView) header.findViewById(R.id.tvName);
            tvSap = (TextView) header.findViewById(R.id.tvSAP);
            tvCourse = (TextView) header.findViewById(R.id.tvCourse);
            tvPercent = (TextView) footer.findViewById(R.id.tvTotalPercent);
            tvClasses = (TextView) footer.findViewById(R.id.tvClass);
            pbPercent = (ProgressBar) footer.findViewById(R.id.pbTotalPercent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mTag = getActivity().getLocalClassName();
        prefs = new MyPreferencesManager(mContext.getApplicationContext());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourses = getResources();
        mExpandedItemTranslationZ =
                mResourses.getDimension(R.dimen.atten_view_expanded_elevation);
    }

    @Override
    public View onCreateView( @NonNull LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        if(container==null)
            return null;

        setHasOptionsMenu(true);
        View mView = inflater.inflate(R.layout.attenview, container, false);
        ButterKnife.inject(this, mView);

        mSwipeRefreshLayout.setSwipeableChildren(android.R.id.list);
        mDropShadow = MainActivity.getInstance().dropShadow;

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4);
        mSwipeRefreshLayout.setProgressViewOffset(true, 1, 92);

        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mListView = getListView();
        mHeader = inflater.inflate(R.layout.list_header, mListView, false);
        mFooter = inflater.inflate(R.layout.list_footer, mListView, false);
        mHFViewHolder = new HeaderFooterViewHolder(mHeader,mFooter);
        mLastRefreshView = (TextView) mHeader.findViewById(R.id.last_refreshed);
        mListView.addHeaderView(mHeader);
        mListView.addFooterView(mFooter);
        mListView.setHeaderDividersEnabled(false);

        DatabaseHandler db = new DatabaseHandler(mContext);
        if(db.getRowCount()<=0) {
            if(getActivity().getIntent().hasExtra(UserAccount.INTENT_EXTRA_USERNAME)) {
                String SAPID = getActivity().getIntent().getExtras().getString(UserAccount.INTENT_EXTRA_USERNAME);
                MySyncManager.addPeriodicSync(mContext, SAPID);
            }
            DataAPI.getAttendance(mContext, successListener(), errorListener());
            mProgress.setVisibility(View.VISIBLE);
        }
        else {
            setAttendance();
            updateLastRefresh();
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                DataAPI.getAttendance(mContext, successListener(), errorListener());
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                mDropShadow.setVisibility(mHeader.isShown() ? View.GONE : View.VISIBLE);
            }
        });
    }

    public void showcaseView() {
        int firstElementPosition = 0;
        firstElementPosition += mListView.getHeaderViewsCount();
        View firstElementView = mListView.getChildAt(firstElementPosition);
        ViewTarget target = firstElementView != null ? new ViewTarget(firstElementView)
                : new ViewTarget(mListView);

        new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(target)
                .setContentTitle(getString(R.string.sv_attendance_title))
                .setContentText(getString(R.string.sv_attendance_content))
                .build();

    }

    protected void updateLastRefresh() {
        mLastRefreshView.setText(getString(R.string.last_refresh, prefs.getLastSyncTime()));
    }

    private void setAttendance() {
        DatabaseHandler db = new DatabaseHandler(getActivity());
        if(db.getRowCount()>0)
        {
            updateHeaderNFooter();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            int expandLimit = Integer.parseInt(sharedPref.getString(
                    getString(R.string.pref_key_sub_limit), "3"));

            List<Subject> subjects = db.getAllOrderedSubjects();

            mAdapter = new ExpandableListAdapter(mContext,subjects,this);
            mAdapter.setLimit(expandLimit);
            mListView.setAdapter(mAdapter);

        }
    }

    private void updateHeaderNFooter() {

        DatabaseHandler db = new DatabaseHandler(mContext);
        ListFooter listfooter = db.getListFooter();
        Float percent = listfooter.getPercentage();

        mHFViewHolder.tvPercent.setText(listfooter.getPercentage()+"%");
        mHFViewHolder.tvClasses.setText(listfooter.getAttended().intValue() + "/" + listfooter.getHeld().intValue());
        mHFViewHolder.pbPercent.setProgress(percent.intValue());
        Drawable d = mHFViewHolder.pbPercent.getProgressDrawable();
        d.setLevel(percent.intValue() * 100);

        ListHeader listheader = db.getListHeader();
        mHFViewHolder.tvName.setText(listheader.getName());
        mHFViewHolder.tvSap.setText(String.valueOf(listheader.getSAPId()));
        mHFViewHolder.tvCourse.setText(listheader.getCourse());

        MainActivity.getInstance().updateDrawerHeader();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(mResourses.getString(R.string.hint_search));

        MenuItemCompat.setOnActionExpandListener(searchItem , new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                DatabaseHandler db = new DatabaseHandler(mContext);
                List<Subject> subjects = db.getAllOrderedSubjects();
                mAdapter.setDataSet(subjects);
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
                DatabaseHandler db = new DatabaseHandler(mContext);
                List<Subject> subjects = db.getAllSubjectsLike(arg0);
                if(mAdapter != null)
                    mAdapter.setDataSet(subjects);
                return false;
            }
        });
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        Activity activity = (Activity) mContext;
        DrawerLayout mDrawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        ListView mDrawerList = (ListView) activity.findViewById(R.id.list_slidermenu);
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
        menu.findItem(R.id.menu_refresh).setVisible(!drawerOpen);
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
                DataAPI.getAttendance(mContext, successListener(), errorListener());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private Response.Listener<String> successListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    new DataAssembler.ParseStudentDetails(mContext, null).execute(response);
                    new DataAssembler.ParseAttendance(mContext, parseListener()).execute(response);
                }
                catch (Exception e) {
                    String msg = mResourses.getString(R.string.unexpected_error);
                    Miscellaneous.showSnackBar(mContext,msg);
                }
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
                mSwipeRefreshLayout.setRefreshing(false);
                if(result == 0) {
                    prefs.setLastSyncTime();
                    updateLastRefresh();
                    setAttendance();
                }
                else
                    MainActivity.getInstance().updateDrawerHeader();
                ErrorHelper.handleError(result, mContext);
            }
        };
    }

    private Response.ErrorListener errorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Stop the refreshing indicator
                if(mProgress == null || mSwipeRefreshLayout == null)
                    return;
                mProgress.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                Miscellaneous.showSnackBar(mContext,msg);
                Log.e(mTag, msg);
            }
        };
    }

    @Override
    public void onItemExpanded(final View view) {
        final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final ExpandableListAdapter.ViewHolder viewHolder = (ExpandableListAdapter.ViewHolder) view.getTag();
        final RelativeLayout childView = viewHolder.childView;
        childView.measure(spec, spec);
        final int startingHeight = view.getHeight();
        final ViewTreeObserver observer = mListView.getViewTreeObserver();
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

                // Figure out how much scrolling is needed to make the view fully visible.
                final Rect localVisibleRect = new Rect();
                view.getLocalVisibleRect(localVisibleRect);
                final int scrollingNeeded = localVisibleRect.top > 0 ? -localVisibleRect.top
                        : endingHeight - localVisibleRect.height();
                final ListView listView = getListView();
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    private int mCurrentScroll = 0;

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

                        if (isExpanded) {
                            if (listView != null) {
                                int scrollBy = (int) (value * scrollingNeeded) - mCurrentScroll;
                                listView.smoothScrollBy(scrollBy, 0);
                                mCurrentScroll += scrollBy;
                            }
                        }
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
        int firstPosition = mListView.getFirstVisiblePosition();
        int lastPosition = mListView.getLastVisiblePosition();

        for (int position = 0; position <= lastPosition - firstPosition; position++) {
            View view = mListView.getChildAt(position);

            if (view != null) {
                final ExpandableListAdapter.ViewHolder viewHolder = (ExpandableListAdapter.ViewHolder) view.getTag();
                if (viewHolder != null && viewHolder.position == callId) {
                    return view;
                }
            }
        }

        return null;
    }

    public void onResume() {
        super.onResume();
        mDropShadow.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        mDropShadow.setVisibility(View.VISIBLE);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
