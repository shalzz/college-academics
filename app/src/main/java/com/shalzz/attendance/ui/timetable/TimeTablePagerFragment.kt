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

package com.shalzz.attendance.ui.timetable

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.bugsnag.android.Bugsnag
import com.github.amlcurran.showcaseview.ShowcaseView
import com.github.amlcurran.showcaseview.targets.Target
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.databinding.FragmentViewpagerBinding
import com.shalzz.attendance.ui.main.MainActivity
import com.shalzz.attendance.utils.RxEventBus
import com.shalzz.attendance.wrapper.DateHelper
import java.util.*
import javax.inject.Inject
import javax.inject.Named

class TimeTablePagerFragment : Fragment(), TimeTableMvpView {

    private lateinit var mViewPager: ViewPager

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    @Inject
    lateinit var mTimeTablePresenter: TimeTablePresenter

    @Inject
    lateinit var mActivity: AppCompatActivity

    @Inject
    lateinit var eventBus: RxEventBus

    private var mPreviousPosition = 15
    private var mAdapter: TimeTablePagerAdapter? = null
    private var mContext: Context? = null
    private var actionbar: ActionBar? = null

    private var _binding: FragmentViewpagerBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private class OnPageChangeListener(private val callback: (position: Int) -> Unit )
        : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            callback(position)
        }
    }

    override fun onStart() {
        super.onStart()
        mTracker.setCurrentScreen(mActivity, javaClass.simpleName, javaClass.simpleName)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (container == null)
            return null
        mContext = activity
        Bugsnag.setContext("Timetable")
        (activity as MainActivity).activityComponent().inject(this)

        setHasOptionsMenu(true)
        retainInstance = false
        _binding = FragmentViewpagerBinding.inflate(inflater, container, false)
        val view = binding.root
        mViewPager = binding.pager

        actionbar = (activity as AppCompatActivity).supportActionBar

        mAdapter = TimeTablePagerAdapter(childFragmentManager,
                mActivity,
                { position -> mViewPager.setCurrentItem(position, true) },
                eventBus)

        mViewPager.offscreenPageLimit = 3
        mViewPager.adapter = mAdapter
        mViewPager.addOnPageChangeListener( OnPageChangeListener(this::updateTitle) )

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter!!.scrollToToday()
    }

    override fun onResume() {
        super.onResume()
        showcaseView()
    }

    private fun showcaseView() {
        val sv = ShowcaseView.Builder(requireActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .singleShot(3333)
                .doNotBlockTouches()
                .setContentTitle(getString(R.string.sv_timetable_title))
                .setContentText(getString(R.string.sv_timetable_content))
                .build()

        sv.overrideButtonClick { sv.hide() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.time_table, menu)
    }

    /* Called whenever we call invalidateOptionsMenu() */
    override fun onPrepareOptionsMenu(menu: Menu) {
        updateTitle(-1)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_date) {
            val today = Date()
            val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            datePickerBuilder.setSelection(today.time)
            val datePicker = datePickerBuilder.build()
            datePicker.addOnPositiveButtonClickListener { selection: Long? ->
                setDate(Date(selection!!))
            }
            datePicker.show(getParentFragmentManager(), datePickerBuilder.toString())

            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.title.toString())
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Scroll to Date")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu")
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)
            return true
        } else if (item.itemId == R.id.menu_today) {
            setDate(Date())

            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.title.toString())
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Scroll to Today")
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu")
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Update action bar title and subtitle
     *
     * @param position to update for, -1 for current page
     */
    private fun updateTitle(position: Int) {
        if (position != -1)
            mPreviousPosition = position
        val mDate = mAdapter!!.getDateForPosition(mPreviousPosition)
        if (mDate != null) {
            actionbar!!.title = DateHelper.getProperWeekday(mDate)
            actionbar!!.subtitle = DateHelper.toProperFormat(mDate)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mTimeTablePresenter.detachView()
        mAdapter!!.destroy()
    }

    /******* MVP View methods implementation  */

    override fun setDate(date: Date) {
        mAdapter!!.setDate(date)
        mAdapter!!.scrollToDate(date)
        updateTitle(-1)
    }

}
