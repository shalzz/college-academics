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

package com.shalzz.attendance.ui.main

import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindArray
import butterknife.BindBool
import butterknife.BindView
import butterknife.ButterKnife
import com.android.billingclient.api.BillingClient.BillingResponse
import com.bugsnag.android.Bugsnag
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.android.material.navigation.NavigationView
import com.shalzz.attendance.BuildConfig
import com.shalzz.attendance.R
import com.shalzz.attendance.billing.BillingManager
import com.shalzz.attendance.billing.BillingProvider
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.ui.attendance.AttendanceListFragment
import com.shalzz.attendance.ui.base.BaseActivity
import com.shalzz.attendance.ui.login.LoginActivity
import com.shalzz.attendance.ui.settings.SettingsFragment
import com.shalzz.attendance.ui.timetable.TimeTablePagerFragment
import com.shalzz.attendance.wrapper.MySyncManager
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class MainActivity : BaseActivity(), MainMvpView, BillingProvider {

    /**
     * Null on tablets
     */
    @BindView(R.id.drawer_layout)
    lateinit var mDrawerLayout: DrawerLayout

    @BindView(R.id.list_slidermenu)
    lateinit var mNavigationView: NavigationView

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    /**
     * Drawer lock state. True for tablets, false otherwise .
     */
    @BindBool(R.bool.tablet_layout)
    @JvmField var isTabletLayout: Boolean = false

    @BindArray(R.array.drawer_array)
    lateinit var mNavTitles: Array<String>

    @Inject lateinit var mDataManager: DataManager

    @Inject lateinit var mMainPresenter: MainPresenter

    @Inject
    lateinit var httpClient: OkHttpClient

    var mPopSettingsBackStack = false

    private var mCurrentSelectedPosition = Fragments.ATTENDANCE.value
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var DrawerheaderVH: DrawerHeaderViewHolder? = null

    private var mFragmentManager: FragmentManager? = null
    private var fragment: Fragment? = null
    // Our custom poor-man's back stack which has only one entry at maximum.
    private var mPreviousFragment: Fragment? = null
    private var mBillingManager: BillingManager? = null

    /**
     * @return currently installed [Fragment] (1-pane has only one at most), or null if none
     * exists.
     */
    private val installedFragment: Fragment?
        get() = mFragmentManager!!.findFragmentByTag(FRAGMENT_TAG)

    private val isAttendanceListInstalled: Boolean
        get() = installedFragment is AttendanceListFragment

    private val isTimeTablePagerInstalled: Boolean
        get() = installedFragment is TimeTablePagerFragment

    private val isSettingsInstalled: Boolean
        get() = installedFragment is SettingsFragment

    /**
     * Reference to fragment positions
     */
    enum class Fragments(val value: Int) {
        ATTENDANCE(1),
        TIMETABLE(2),
        SETTINGS(3)
    }

    class DrawerHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.drawer_header_name)
        lateinit  var tv_name: TextView
        @BindView(R.id.drawer_header_course)
        lateinit  var tv_course: TextView
        @BindView(R.id.last_refreshed)
        lateinit  var last_refresh: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer)
        ButterKnife.bind(this)
        activityComponent().inject(this)
        Bugsnag.setContext("MainActivity")
        mMainPresenter.attachView(this)

        mFragmentManager = supportFragmentManager
        DrawerheaderVH = DrawerHeaderViewHolder(mNavigationView.getHeaderView(0))
        mBillingManager = BillingManager(this, mDataManager,
                mMainPresenter.updateListener)
        setSupportActionBar(mToolbar)

        // Set the list's click listener
        mNavigationView.setNavigationItemSelectedListener(NavigationItemSelectedListener())

        initDrawer()
        init(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        showcaseView()
        // Note: We query purchases in onResume() to handle purchases completed while the activity
        // is inactive. For example, this can happen if the activity is destroyed during the
        // purchase flow. This ensures that when the activity is resumed it reflects the user's
        // current purchases.
        if (mBillingManager != null && mBillingManager!!.billingClientResponseCode == BillingResponse.OK) {
            mBillingManager!!.queryPurchases()
        }
    }

    /**
     * Initialise a fragment
     */
    fun init(bundle: Bundle?) {

        // Select either the default item (Fragments.ATTENDANCE) or the last selected item.
        mCurrentSelectedPosition = reloadCurrentFragment()

        // Recycle fragment
        if (bundle != null) {
            fragment = mFragmentManager!!.findFragmentByTag(FRAGMENT_TAG)
            mPreviousFragment = mFragmentManager!!.getFragment(bundle, PREVIOUS_FRAGMENT_TAG)
            Timber.d("current fag found: %s", fragment)
            Timber.d("previous fag found: %s", mPreviousFragment)
            selectItem(mCurrentSelectedPosition)
            showFragment(fragment!!)
        } else {

            if (intent.hasExtra(LAUNCH_FRAGMENT_EXTRA)) {
                mCurrentSelectedPosition = intent.getIntExtra(LAUNCH_FRAGMENT_EXTRA,
                        Fragments.ATTENDANCE.value)
            } else if (intent.action != null && intent.action == Intent.ACTION_MANAGE_NETWORK_USAGE) {
                mCurrentSelectedPosition = Fragments.SETTINGS.value
                Timber.i("MANAGE_NETWORK_USAGE intent received")
            }
            displayView(mCurrentSelectedPosition)
        }

        mMainPresenter.loadUser()
    }

    private fun initDrawer() {
        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
        mDrawerToggle!!.isDrawerIndicatorEnabled = true
        mToolbar.setNavigationOnClickListener {
            val drawerLockMode = mDrawerLayout.getDrawerLockMode(GravityCompat.START)
            // check if drawer is shown as up
            if (drawerLockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                onBackPressed()
            } else if (mDrawerLayout.isDrawerVisible(GravityCompat.START) && drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_OPEN) {
                mDrawerLayout.closeDrawer(GravityCompat.START)
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START)
            }
        }
        mDrawerLayout.addDrawerListener(mDrawerToggle as ActionBarDrawerToggle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mDrawerLayout.setStatusBarBackgroundColor(resources.getColor(R.color.primary_dark,
                    theme))
        } else {

            mDrawerLayout.setStatusBarBackgroundColor(resources.getColor(
                    R.color.primary_dark))
        }
    }

    private fun showcaseView() {

        if (isTabletLayout) {
            if (fragment is AttendanceListFragment) {
                (fragment as AttendanceListFragment).showcaseView()
            }
            return
        }

        val homeTarget = {
            // Get approximate position of home icon's center
            val actionBarSize = mToolbar.height
            val x = actionBarSize / 2
            val y = actionBarSize / 2
            Point(x, y)
        }

        val sv = ShowcaseView.Builder(this)
                .setTarget(homeTarget)
                .setStyle(R.style.ShowcaseTheme)
                .singleShot(1111)
                .setContentTitle(getString(R.string.sv_main_activity_title))
                .setContentText(getString(R.string.sv_main_activity_content))
                .build()

        sv.overrideButtonClick {
            mDrawerLayout.closeDrawer( mNavigationView as View)
            sv.hide()
            if (fragment is AttendanceListFragment) {
                (fragment as AttendanceListFragment).showcaseView()
            }
        }
    }

    fun setDrawerAsUp(enabled: Boolean) {
        val start = if (enabled) 0f else 1f
        val end = if (enabled) 1f else 0f
        mDrawerLayout.setDrawerLockMode(if (enabled)
            DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        else
            DrawerLayout.LOCK_MODE_UNLOCKED)

        val anim = ValueAnimator.ofFloat(start, end)
        anim.addUpdateListener { valueAnimator ->
            val slideOffset = valueAnimator.animatedValue as Float
            mDrawerToggle!!.onDrawerSlide(mDrawerLayout, slideOffset)
        }
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 300
        anim.start()
    }

    private inner class NavigationItemSelectedListener : NavigationView.OnNavigationItemSelectedListener {
        override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
            displayView(menuItem.order)
            return false
        }
    }

    internal fun displayView(position: Int) {
        val actionBar = supportActionBar
        // update the main content by replacing fragments
        when (position) {
            0 -> return
            1 -> {
                fragment = AttendanceListFragment()
                mPreviousFragment = null // GC
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false)
                }
            }
            2 -> {
                fragment = TimeTablePagerFragment()
                mPreviousFragment = null // GC
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false)
                }
            }
            3 -> {
                fragment = SettingsFragment()
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true)
                }
            }
            else -> {
            }
        }

        if (fragment != null) {
            selectItem(position)
            showFragment(fragment!!)
        } else {
            Timber.e("Error in creating fragment")
        }
    }

    /**
     * Update selected item and title, then close the drawer
     * @param position the item to highlight
     */
    private fun selectItem(position: Int) {
        mCurrentSelectedPosition = position
        mNavigationView.menu.getItem(position - 1).isChecked = true
        title = mNavTitles[position - 1]
        if (mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.closeDrawer(mNavigationView)
    }

    /**
     * Push the installed fragment into our custom back stack (or optionally
     * [FragmentTransaction.remove] it) and [FragmentTransaction.add] `fragment`.
     *
     * @param fragment [Fragment] to be added.
     */
    private fun showFragment(fragment: Fragment) {
        val ft = mFragmentManager!!.beginTransaction()
        val installed = installedFragment

        // return if the fragment is already installed
        if (isAttendanceListInstalled && fragment is AttendanceListFragment ||
                isTimeTablePagerInstalled && fragment is TimeTablePagerFragment ||
                isSettingsInstalled && fragment is SettingsFragment) {
            return
        }

        if (mPreviousFragment != null) {
            Timber.d("showFragment: destroying previous fragment %s",
                    mPreviousFragment!!.javaClass.simpleName)
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            ft.remove(mPreviousFragment!!)
            mPreviousFragment = null
        }

        // Remove the current fragment and push it into the backstack.
        if (installed != null) {
            mPreviousFragment = installed
            ft.detach(mPreviousFragment!!)
        }

        // Show the new one
        ft.add(R.id.frame_container, fragment, FRAGMENT_TAG)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        ft.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = MenuInflater(this)
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // called by the activity on tablets,
        // as we do not set a onClick listener
        // on the toolbar navigation icon
        // while on a tablet
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.menu_logout) {
            mMainPresenter.logout()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // close drawer if it is open
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView)
        } else if (shouldPopFromBackStack()) {
            if (mPopSettingsBackStack) {
                Timber.i("Back: Popping from internal back stack")
                mPopSettingsBackStack = false
                mFragmentManager!!.popBackStackImmediate()
                setDrawerAsUp(false)
            } else {
                Timber.i("Back: Popping from custom back stack")
                // Custom back stack
                popFromBackStack()
                val actionBar = supportActionBar
                if (isTabletLayout && actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(false)
                }
            }
        } else {
            ActivityCompat.finishAfterTransition(this)
            Timber.i("Back: App closed")
        }
    }

    /**
     * @return true if we should pop from our custom back stack.
     */
    private fun shouldPopFromBackStack(): Boolean {

        if (mPreviousFragment == null) {
            return false // Nothing in the back stack
        }
        val installed = installedFragment
                ?: // If no fragment is installed right now, do nothing.
                return false
// Okay now we have 2 fragments; the one in the back stack and the one that's currently
        // installed.
        return !(installed is AttendanceListFragment || installed is TimeTablePagerFragment)

    }

    /**
     * Pop from our custom back stack.
     */
    private fun popFromBackStack() {
        if (mPreviousFragment == null) {
            return
        }
        val ft = mFragmentManager!!.beginTransaction()
        val installed = installedFragment
        var position = Fragments.ATTENDANCE.value
        Timber.i("backstack: [pop] %s -> %s", installed!!.javaClass.simpleName,
                mPreviousFragment!!.javaClass.simpleName)

        ft.remove(installed)
        ft.attach(mPreviousFragment!!)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
        ft.commit()

        // redraw fragment
        if (mPreviousFragment is AttendanceListFragment) {
            position = Fragments.ATTENDANCE.value
        } else if (mPreviousFragment is TimeTablePagerFragment) {
            position = Fragments.TIMETABLE.value
            //((TimeTablePagerFragment) mPreviousFragment).updateFragmentsData();
        }
        selectItem(position)
        mPreviousFragment = null
    }

    private fun persistCurrentFragment() {
        if (!LOGGED_OUT) {
            val editor = getSharedPreferences("SETTINGS", 0).edit()
            mCurrentSelectedPosition = if (mCurrentSelectedPosition == Fragments.SETTINGS.value)
                Fragments.ATTENDANCE.value
            else
                mCurrentSelectedPosition
            editor.putInt(PREFERENCE_ACTIVATED_FRAGMENT, mCurrentSelectedPosition).commit()
        }
    }

    private fun reloadCurrentFragment(): Int {
        val settings = getSharedPreferences("SETTINGS", 0)
        return settings.getInt(PREFERENCE_ACTIVATED_FRAGMENT, Fragments.ATTENDANCE.value)
    }

    override fun setTitle(title: CharSequence) {
        mToolbar.title = title
        mToolbar.subtitle = ""
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle!!.syncState()

        // Toolbar#setTitle is called by the system on onCreate and
        // again over here which sets the activity label
        // as the title.
        // So we need to call setTitle again as well
        // to show the correct title.
        title = mNavTitles[mCurrentSelectedPosition - 1]
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // for orientation changes, etc.
        if (mPreviousFragment != null) {
            mFragmentManager!!.putFragment(outState, PREVIOUS_FRAGMENT_TAG, mPreviousFragment!!)
            Timber.d("previous fag saved: %s", mPreviousFragment!!.javaClass.simpleName)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (mDrawerToggle != null)
            mDrawerToggle!!.onConfigurationChanged(newConfig)
    }

    public override fun onPause() {
        persistCurrentFragment()
        super.onPause()
    }

    public override fun onDestroy() {
        mDrawerLayout.removeDrawerListener(mDrawerToggle as ActionBarDrawerToggle)
        if (mBillingManager != null) {
            mBillingManager!!.destroy()
        }
        mMainPresenter.detachView()
        super.onDestroy()
    }

    /****** BillingProvider interface implementations */

    override fun getBillingManager(): BillingManager? {
        return mBillingManager
    }

    override fun isProKeyPurchased(): Boolean {
        return mMainPresenter.isProKeyPurchased
    }

    /******* MVP View methods implementation  */

    override fun updateUserDetails(user: User) {
        if (!user.name.isEmpty())
            DrawerheaderVH!!.tv_name.text = user.name
        if (!user.course.isEmpty())
            DrawerheaderVH!!.tv_course.text = user.course
    }

    override fun logout() {
        // Remove Sync Account
        MySyncManager.removeSyncAccount(this)

        // Invalidate the complete network cache
        try {
            httpClient.cache().evictAll()
        } catch (e: IOException) {
            Timber.e(e)
        }

        // Cancel a notification if it is shown.
        val mNotificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(0 /* timetable changed notification id */)

        // Destroy current activity and start doLogin Activity
        val ourIntent = Intent(this, LoginActivity::class.java)
        startActivity(ourIntent)
        finish()
    }

    companion object {

        /**
         * To prevent saving the drawer position when logging out.
         */
        var LOGGED_OUT = false

        /**
         * Remember the position of the selected item.
         */
        val PREFERENCE_ACTIVATED_FRAGMENT = "ACTIVATED_FRAGMENT2.2"

        val FRAGMENT_TAG = "MainActivity.FRAGMENT"

        val LAUNCH_FRAGMENT_EXTRA = BuildConfig.APPLICATION_ID + ".MainActivity.LAUNCH_FRAGMENT"

        private val PREVIOUS_FRAGMENT_TAG = "MainActivity.PREVIOUS_FRAGMENT"
    }
}
