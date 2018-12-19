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

package com.shalzz.attendance.ui.day

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

import com.google.android.material.snackbar.Snackbar
import com.malinskiy.materialicons.IconDrawable
import com.malinskiy.materialicons.Iconify
import com.shalzz.attendance.R
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.injection.ActivityContext
import com.shalzz.attendance.ui.main.MainActivity
import com.shalzz.attendance.utils.DividerItemDecoration
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.wrapper.MultiSwipeRefreshLayout

import java.util.Date

import javax.inject.Inject
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.empty_view.*
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.fragment_day.view.*
import timber.log.Timber

class DayFragment : Fragment(), DayMvpView {

    @Inject
    lateinit var mDayPresenter: DayPresenter

    @Inject
    lateinit var mAdapter: DayListAdapter

    @Inject
    @field:ActivityContext
    lateinit var mContext: Context

    class EmptyView(view: View) {
        var imageView: ImageView = view.emptyStateImageView
        var titleTextView: TextView = view.emptyStateTitleTextView
        var contentTextView: TextView = view.emptyStateContentTextView
        var button: Button = view.emptyStateButton
    }

    /**
     * The [androidx.swiperefreshlayout.widget.SwipeRefreshLayout] that detects swipe gestures and
     * triggers callbacks in the app.
     */
    var mSwipeRefreshLayout: MultiSwipeRefreshLayout? = null
    var mRecyclerView: RecyclerView? = null
    private lateinit var mDate: Date
    private lateinit var mEmptyView: EmptyView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(R.layout.fragment_day, container, false)
        (activity as MainActivity).activityComponent().inject(this)

        mRecyclerView = mView.time_table_recycler_view
        mSwipeRefreshLayout = mView.swiperefresh
        mEmptyView = EmptyView(emptyView)

        setHasOptionsMenu(true)
        mDayPresenter.attachView(this)

        mDate = if (arguments!!.getSerializable(ARG_DATE) != null)
            arguments!!.getSerializable(ARG_DATE) as Date
        else
            Date()

        mSwipeRefreshLayout!!.setSwipeableChildren(R.id.time_table_recycler_view)
        mSwipeRefreshLayout!!.setColorSchemeResources(
                R.color.swipe_color_1, R.color.swipe_color_2,
                R.color.swipe_color_3, R.color.swipe_color_4)
        mSwipeRefreshLayout!!.setOnRefreshListener { mDayPresenter.syncDay(mDate) }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView!!.setHasFixedSize(true)

        // use a linear layout manager
        val mLayoutManager = LinearLayoutManager(mContext,
                RecyclerView.VERTICAL, false)
        mLayoutManager.isSmoothScrollbarEnabled = true
        mRecyclerView!!.layoutManager = mLayoutManager

        val itemDecoration = DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST)
        mRecyclerView!!.addItemDecoration(itemDecoration)

        mRecyclerView!!.adapter = mAdapter

        mDayPresenter.loadDay(mDate)

        return mView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.day, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == R.id.menu_refresh) {
            // We make sure that the SwipeRefreshLayout is displaying it's refreshing indicator
            if (!mSwipeRefreshLayout!!.isRefreshing) {
                mSwipeRefreshLayout!!.isRefreshing = true
                mDayPresenter.syncDay(mDate)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDayPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun setRefreshing() {
        if (!mSwipeRefreshLayout!!.isRefreshing) {
            mSwipeRefreshLayout!!.isRefreshing = true
        }
    }

    override fun stopRefreshing() {
        mSwipeRefreshLayout!!.isRefreshing = false
    }

    override fun clearDay() {
        showNoTimetableEmptyView()
        mAdapter.clear()
    }

    override fun setDay(day: List<Period>) {
        showEmptyView(false)
        mAdapter.update(day)
    }

    override fun showEmptyView(show: Boolean) {
        stopRefreshing()
        if (show) {
            emptyView!!.visibility = View.VISIBLE
        } else {
            emptyView!!.visibility = View.GONE
        }
    }

    override fun showNoTimetableEmptyView() {
        mEmptyView.imageView.visibility = View.GONE
        mEmptyView.titleTextView.setText(R.string.no_classes)
        mEmptyView.contentTextView.setText(R.string.swipe_info)
        mEmptyView.button.visibility = View.GONE

        showEmptyView(true)
    }

    override fun showNoConnectionErrorView() {
        mEmptyView.imageView.visibility = View.VISIBLE
        val emptyDrawable = IconDrawable(mContext,
                Iconify.IconValue.zmdi_wifi_off)
                .colorRes(android.R.color.darker_gray)
        mEmptyView.imageView.setImageDrawable(emptyDrawable)
        mEmptyView.titleTextView.setText(R.string.no_connection_title)
        mEmptyView.contentTextView.setText(R.string.no_connection_content)
        mEmptyView.button.setOnClickListener { mDayPresenter.syncDay(mDate) }
        mEmptyView.button.visibility = View.VISIBLE

        showEmptyView(true)
    }

    override fun showNetworkErrorView(error: String) {
        val emptyDrawable = IconDrawable(mContext,
                Iconify.IconValue.zmdi_cloud_off)
                .colorRes(android.R.color.darker_gray)
        mEmptyView.imageView.setImageDrawable(emptyDrawable)
        mEmptyView.titleTextView.setText(R.string.network_error_message)
        mEmptyView.contentTextView.text = error
        mEmptyView.button.setOnClickListener { mDayPresenter.syncDay(mDate) }
        mEmptyView.button.visibility = View.VISIBLE

        showEmptyView(true)
    }

    override fun showError(message: String) {
        Timber.d("Error: %s", message)
        stopRefreshing()
        Miscellaneous.showSnackBar(mSwipeRefreshLayout, message)
    }

    override fun showRetryError(message: String) {
        stopRefreshing()
        Snackbar.make(mRecyclerView!!, message, Snackbar.LENGTH_LONG)
                .setAction("Retry") { mDayPresenter.syncDay(mDate) }
                .show()
    }

    companion object {

        private const val ARG_DATE = "date"

        /**
         * Create a new instance of DayFragment, providing "Date"
         * as an argument.
         */
        fun newInstance(date: Date): DayFragment {
            val f = DayFragment()

            val args = Bundle()
            args.putSerializable(ARG_DATE, date)
            f.arguments = args

            return f
        }
    }
}
