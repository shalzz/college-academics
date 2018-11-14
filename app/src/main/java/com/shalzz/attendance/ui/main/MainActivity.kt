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

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient.BillingResponse
import com.bugsnag.android.Bugsnag
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.android.material.navigation.NavigationView
import com.shalzz.attendance.BuildConfig
import com.shalzz.attendance.R
import com.shalzz.attendance.billing.BillingManager
import com.shalzz.attendance.billing.BillingProvider
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.sync.MyAccountManager
import com.shalzz.attendance.ui.attendance.AttendanceListFragment
import com.shalzz.attendance.ui.base.BaseActivity
import com.shalzz.attendance.ui.login.AuthenticatorActivity
import kotlinx.android.synthetic.main.drawer.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.include_drawer_list.*
import kotlinx.android.synthetic.main.include_toolbar.*
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class MainActivity : BaseActivity(), MainMvpView, BillingProvider {

    /**
     * Null on tablets
     */
    private lateinit var mNavigationView: NavigationView
    private lateinit var mToolbar: Toolbar

    /**
     * Drawer lock state. True for tablets, false otherwise .
     */
    private var isTabletLayout: Boolean = false

    @Inject lateinit var mDataManager: DataManager
    @Inject lateinit var mMainPresenter: MainPresenter
    @Inject lateinit var mPreferencesHelper: PreferencesHelper
    @Inject lateinit var httpClient: OkHttpClient

    private var mCurrentSelectedPosition = Fragments.ATTENDANCE.value
    private var drawerHeaderVH: DrawerHeaderViewHolder? = null

    private var fragment: Fragment? = null
    private var mBillingManager: BillingManager? = null

    /**
     * Reference to fragment positions
     */
    enum class Fragments(val value: Int) {
        ATTENDANCE(1),
        TIMETABLE(2),
        SETTINGS(3)
    }

    class DrawerHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.drawer_header_name
        val tvCourse: TextView = itemView.drawer_header_course
        val lastRefresh: TextView = itemView.last_refreshed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer)
        Bugsnag.setContext("MainActivity")

        activityComponent().inject(this)
        mMainPresenter.attachView(this)
        mBillingManager = BillingManager(this, mDataManager, mMainPresenter.updateListener)

        mNavigationView = list_slidermenu
        mToolbar = toolbar
        isTabletLayout = resources.getBoolean(R.bool.tablet_layout)

        setSupportActionBar(toolbar)
        val navController = Navigation.findNavController(this, R.id.nav_main_host_fragment)
        NavigationUI.setupWithNavController(mNavigationView, navController)

        if (!isTabletLayout) {
            val appBarConfiguration = AppBarConfiguration(
                setOf(R.id.attendanceListFragment, R.id.timeTablePagerFragment),
                drawer_layout
            )
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)
        }

        drawerHeaderVH = DrawerHeaderViewHolder(mNavigationView.getHeaderView(0))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawer_layout.setStatusBarBackgroundColor(
                resources.getColor(R.color.primary_dark, theme))
        } else {
            drawer_layout.setStatusBarBackgroundColor(
                resources.getColor(R.color.primary_dark))
        }

        if (intent.hasExtra(LAUNCH_FRAGMENT_EXTRA)) {
            mCurrentSelectedPosition = intent.getIntExtra(LAUNCH_FRAGMENT_EXTRA,
                Fragments.ATTENDANCE.value)
        } else if (intent.action != null && intent.action == Intent.ACTION_MANAGE_NETWORK_USAGE) {
            mCurrentSelectedPosition = Fragments.SETTINGS.value // TODO: manual
            Timber.i("MANAGE_NETWORK_USAGE intent received")
        }

        mMainPresenter.loadUser(mPreferencesHelper.userId!!)
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
            drawer_layout.closeDrawer( mNavigationView as View)
            sv.hide()
            if (fragment is AttendanceListFragment) {
                (fragment as AttendanceListFragment).showcaseView()
            }
        }
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

    override fun setTitle(title: CharSequence) {
        mToolbar.title = title
        mToolbar.subtitle = ""
    }

    public override fun onDestroy() {
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
            drawerHeaderVH!!.tvName.text = user.name
        if (!user.course.isEmpty())
            drawerHeaderVH!!.tvCourse.text = user.course
    }

    override fun logout() {
        // Remove Sync Account
        MyAccountManager.removeSyncAccount(this)

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

        // Destroy current activity and start Login Activity
        val ourIntent = Intent(this, AuthenticatorActivity::class.java)
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
