/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.ui.attendance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bugsnag.android.Bugsnag
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.ViewTarget
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.malinskiy.materialicons.IconDrawable
import com.malinskiy.materialicons.Iconify
import com.shalzz.attendance.R
import com.shalzz.attendance.data.model.ListFooter
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.ui.main.MainActivity
import com.shalzz.attendance.utils.DividerItemDecoration
import com.shalzz.attendance.utils.Utils
import kotlinx.android.synthetic.main.empty_view.*
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.fragment_attendance.*
import kotlinx.android.synthetic.main.fragment_attendance.view.*
import javax.inject.Inject
import javax.inject.Named

class AttendanceListFragment : Fragment(), AttendanceMvpView,
    ExpandableListAdapter.SubjectItemExpandedListener {

    private val GRID_LAYOUT_SPAN_COUNT = 2

    @Inject
    lateinit var mPresenter: AttendancePresenter

    @Inject
    lateinit var mAdapter: ExpandableListAdapter

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    private var useGridLayout: Boolean = false
    private var mExpandCollapseDuration: Int = 0

    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mGridLayoutManager: StaggeredGridLayoutManager? = null
    private lateinit var mActivity: Activity

    private lateinit var mEmptyView: EmptyView

    private class EmptyView(view : View) {
        var imageView: ImageView = view.emptyStateImageView
        var titleTextView: TextView = view.emptyStateTitleTextView
        var contentTextView: TextView = view.emptyStateContentTextView
        var button: Button = view.emptyStateButton
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mView = inflater.inflate(R.layout.fragment_attendance, container, false)
        mEmptyView = EmptyView(mView.emptyView)
        Bugsnag.setContext("AttendanceList")

        useGridLayout = resources.getBoolean(R.bool.use_grid_layout)
        mExpandCollapseDuration = resources.getInteger(R.integer.expand_collapse_duration)

        mActivity = activity!!
        (mActivity as MainActivity).activityComponent().inject(this)
        mPresenter.attachView(this)
        setHasOptionsMenu(true)

        mView.layoutSwipeRefresh.setOnRefreshListener { mPresenter.syncAttendance() }
        mView.layoutSwipeRefresh.setSwipeableChildren(R.id.recyclerView)

        // Set the color scheme of the SwipeRefreshLayout by providing 4 color resource ids
        mView.layoutSwipeRefresh.setColorSchemeResources(
            R.color.swipe_color_1, R.color.swipe_color_2,
            R.color.swipe_color_3, R.color.swipe_color_4
        )

        if (useGridLayout) {
            mGridLayoutManager = StaggeredGridLayoutManager(
                GRID_LAYOUT_SPAN_COUNT,
                StaggeredGridLayoutManager.VERTICAL
            )
            mGridLayoutManager!!.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        } else {
            mLinearLayoutManager = LinearLayoutManager(
                mActivity,
                RecyclerView.VERTICAL, false
            )
            mLinearLayoutManager!!.isSmoothScrollbarEnabled = false
            mLinearLayoutManager!!.stackFromEnd = false
        }

        mView.recyclerView.setLayoutManager(if (useGridLayout) mGridLayoutManager else mLinearLayoutManager)

        val itemDecoration = DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL_LIST)
        mView.recyclerView.addItemDecoration(itemDecoration)

        val mFooter = inflater.inflate(R.layout.list_footer, mView.recyclerView, false)
        mFooter.visibility = View.INVISIBLE
        mAdapter.addFooter(mFooter)
        mAdapter.setCallback(this)

        mView.recyclerView.adapter = mAdapter

        mPresenter.loadAttendance(null)

        return mView
    }

    override fun onStart() {
        super.onStart()
        mTracker.setCurrentScreen(mActivity, javaClass.simpleName, javaClass.simpleName)
    }

    override fun onResume() {
        super.onResume()
        (mActivity as MainActivity).setTitle(R.string.navigation_item_1)
    }

    override fun onCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater?) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater!!.inflate(R.menu.attendance, menu)
        val searchItem = menu!!.findItem(R.id.menu_search)

        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = resources.getString(R.string.hint_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(arg0: String): Boolean {
                Utils.closeKeyboard(mActivity, searchView)
                return false
            }

            override fun onQueryTextChange(arg0: String): Boolean {
                val filter = if (!TextUtils.isEmpty(arg0)) arg0 else null
                clearSubjects()
                mPresenter.loadAttendance(filter)
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, filter)
                mTracker.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
                return false
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!layoutSwipeRefresh.isRefreshing) {
                layoutSwipeRefresh.isRefreshing = true
                mPresenter.syncAttendance()
            }
            return true
        } else if (item.itemId == R.id.menu_search) {
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.title.toString())
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Search")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu")
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemExpanded(view: View) {
        val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val viewHolder = view.tag as ExpandableListAdapter.GenericViewHolder
        val childView = viewHolder.childView
        childView!!.measure(spec, spec)
        val startingHeight = view.height
        val observer = recyclerView.viewTreeObserver
        observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                // We don'mTracker want to continue getting called for every draw.
                if (observer.isAlive) {
                    observer.removeOnPreDrawListener(this)
                }
                // Calculate some values to help with the animation.
                val endingHeight = view.height
                val distance = Math.abs(endingHeight - startingHeight)
                val baseHeight = Math.min(endingHeight, startingHeight)
                val isExpanded = endingHeight > startingHeight

                // Set the views back to the start state of the animation
                view.layoutParams.height = startingHeight
                if (!isExpanded) {
                    viewHolder.childView!!.visibility = View.VISIBLE
                }

                // Set up the animator to animate the expansion and shadow depth.
                val animator = if (isExpanded)
                    ValueAnimator.ofFloat(0f, 1f)
                else
                    ValueAnimator.ofFloat(1f, 0f)

                // scroll to make the view fully visible.
                recyclerView.smoothScrollToPosition(viewHolder.adapterPosition)

                animator.addUpdateListener { animator1 ->
                    val value = animator1.animatedValue as Float

                    // For each value from 0 to 1, animate the various parts of the layout.
                    view.layoutParams.height = (value * distance + baseHeight).toInt()
                    view.requestLayout()
                }

                // Set everything to their final values when the animation's done.
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

                        if (!isExpanded) {
                            viewHolder.childView!!.visibility = View.GONE
                        }
                    }
                })

                animator.duration = mExpandCollapseDuration.toLong()
                animator.start()

                // Return false so this draw does not occur to prevent the final frame from
                // being drawn for the single frame before the animations start.
                return false
            }
        })
    }

    override fun getViewForCallId(callId: Long): View? {
        if (!useGridLayout) {
            assert(mLinearLayoutManager != null)
            val firstPosition = mLinearLayoutManager!!.findFirstVisibleItemPosition()
            val lastPosition = mLinearLayoutManager!!.findLastVisibleItemPosition()

            for (position in 0..lastPosition - firstPosition) {
                val view = recyclerView.getChildAt(position)

                if (view != null) {
                    val viewHolder = view.tag as ExpandableListAdapter.GenericViewHolder
                    if (viewHolder.layoutPosition.toLong() == callId) {
                        return view
                    }
                }
            }
        } else {
            val firstPosition = intArrayOf(0, 0)
            val lastPosition = intArrayOf(0, 0)
            assert(mGridLayoutManager != null)
            mGridLayoutManager!!.findFirstVisibleItemPositions(firstPosition)
            mGridLayoutManager!!.findLastVisibleItemPositions(lastPosition)

            for (i in 0 until GRID_LAYOUT_SPAN_COUNT) {
                for (position in 0..lastPosition[i] - firstPosition[i]) {
                    val view = recyclerView.getChildAt(position)

                    if (view != null) {
                        val viewHolder = view.tag as ExpandableListAdapter.GenericViewHolder
                        if (viewHolder.layoutPosition.toLong() == callId) {
                            return view
                        }
                    }
                }
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun clearSubjects() {
        mAdapter.clear()
    }

    override fun addSubjects(subjects: List<Subject>) {
        stopRefreshing()
        showEmptyView(false)
        mAdapter.addAll(subjects)
    }

    override fun updateFooter(footer: ListFooter) {
        mAdapter.updateFooter(footer)
    }

    override fun showcaseView() {
        if (recyclerView.getChildAt(2) != null) {
            val target = ViewTarget(recyclerView.getChildAt(2))

            ShowcaseView.Builder(activity!!)
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(target)
                .singleShot(2222)
                .setContentTitle(getString(R.string.sv_attendance_title))
                .setContentText(getString(R.string.sv_attendance_content))
                .build()
        }
    }

    override fun setRefreshing() {
        progressCircular.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    override fun stopRefreshing() {
        progressCircular.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        layoutSwipeRefresh.isRefreshing = false
    }

    override fun showError(message: String) {
        stopRefreshing()
        Utils.showSnackBar(recyclerView, message)
    }

    override fun showRetryError(message: String) {
        stopRefreshing()
        Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") { v -> mPresenter.syncAttendance() }
            .show()
    }

    override fun showEmptyView(show: Boolean) {
        stopRefreshing()
        if (show) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun showNetworkErrorView(error: String) {
        val emptyDrawable = IconDrawable(
            mActivity,
            Iconify.IconValue.zmdi_cloud_off
        )
            .colorRes(android.R.color.darker_gray)
        mEmptyView.imageView.setImageDrawable(emptyDrawable)
        mEmptyView.titleTextView.text = "Network Error"
        mEmptyView.contentTextView.text = error
        mEmptyView.button.setOnClickListener { mPresenter.syncAttendance() }
        mEmptyView.button.visibility = View.VISIBLE

        showEmptyView(true)
    }

    override fun showNoConnectionErrorView() {
        val emptyDrawable = IconDrawable(
            mActivity,
            Iconify.IconValue.zmdi_wifi_off
        )
            .colorRes(android.R.color.darker_gray)
        mEmptyView.imageView.setImageDrawable(emptyDrawable)
        mEmptyView.titleTextView.setText(R.string.no_connection_title)
        mEmptyView.contentTextView.setText(R.string.no_connection_content)
        mEmptyView.button.setOnClickListener { mPresenter.syncAttendance() }
        mEmptyView.button.visibility = View.VISIBLE

        showEmptyView(true)
    }

    override fun showEmptyErrorView() {
        val emptyDrawable = IconDrawable(
            mActivity,
            Iconify.IconValue.zmdi_cloud_off
        )
            .colorRes(android.R.color.darker_gray)
        mEmptyView.imageView.setImageDrawable(emptyDrawable)
        mEmptyView.titleTextView.setText(R.string.no_data_title)
        mEmptyView.contentTextView.setText(R.string.no_data_content)
        mEmptyView.button.visibility = View.GONE

        showEmptyView(true)
    }
}
