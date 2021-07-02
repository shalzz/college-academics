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

package com.shalzz.attendance.ui.main

import android.app.Activity
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
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.bugsnag.android.Bugsnag
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.android.material.navigation.NavigationView
import com.shalzz.attendance.MyApplication
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
import com.shalzz.attendance.utils.Utils
import kotlinx.android.synthetic.main.drawer.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.include_drawer_list.*
import kotlinx.android.synthetic.main.include_toolbar.*
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.lang.ref.WeakReference
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

    private var drawerHeaderVH: DrawerHeaderViewHolder? = null

    private var fragment: Fragment? = null
    private var mBillingManager: BillingManager? = null
    private lateinit var navController: NavController

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

        val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_main_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupWithNavController(mNavigationView, navController)

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

        if (intent.action != null && intent.action == Intent.ACTION_MANAGE_NETWORK_USAGE) {
            navController.navigate(R.id.settingsFragment)
            Timber.i("MANAGE_NETWORK_USAGE intent received")
        }

        mMainPresenter.loadUser(mPreferencesHelper.userId!!)

        if (!mPreferencesHelper.loginStatus) {
            val ourIntent = Intent(this, AuthenticatorActivity::class.java)
            ourIntent.putExtra(AuthenticatorActivity.ARG_AUTH_TYPE, MyAccountManager.AUTHTOKEN_TYPE_READ_ONLY)
            startActivityForResult(ourIntent, ACTIVITY_RESULT_CODE_AUTHENTICATION)
        }
    }

    override fun onResume() {
        super.onResume()
        showcaseView()
        // Note: We query purchases in onResume() to handle purchases completed while the activity
        // is inactive. For example, this can happen if the activity is destroyed during the
        // purchase flow. This ensures that when the activity is resumed it reflects the user's
        // current purchases.
        if (mBillingManager != null && mBillingManager!!.billingClientResponseCode ==
            BillingResponseCode.OK) {
            mBillingManager!!.queryPurchases()
        }
    }

    private fun setupWithNavController(
        navigationView: NavigationView,
        navController: NavController
    ) {
        navigationView.setNavigationItemSelectedListener { item ->
            if (navController.currentDestination!!.id != item.itemId) {
                if (item.itemId == R.id.helpNSupport) {
                    MyApplication.helpStack.showHelp(this)
                } else
                    NavigationUI.onNavDestinationSelected(item, navController)
            }
            if (!isTabletLayout) {
                val parent = navigationView.parent
                (parent as DrawerLayout).closeDrawer(navigationView)
            }
            true
        }
        val weakReference = WeakReference(navigationView)
        navController.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {

            override fun onDestinationChanged(controller: NavController,
                                              destination: NavDestination,
                                              arguments: Bundle?) {
                val view = weakReference.get()
                if (view == null) {
                    controller.removeOnDestinationChangedListener(this)
                    return
                }
                val menu = view.menu
                var h = 0

                while (h < menu.size()) {
                    val item = menu.getItem(h)
                    item.isChecked = matchDestination(destination, item.itemId)
                    ++h
                }
            }
        })
    }

    internal fun matchDestination(
         destination: NavDestination,
         destId: Int
    ): Boolean {
        var currentDestination: NavDestination? = destination
        while (currentDestination!!.id != destId && currentDestination.parent != null) {
            currentDestination = currentDestination.parent
        }
        return currentDestination.id == destId
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
            drawer_layout.closeDrawer(mNavigationView)
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
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_logout -> {
                Utils.showSnackBar(mToolbar, "Logging out...")
                mMainPresenter.logout()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED)
            finish()
        else
            navController.navigate(R.id.attendanceListFragment)
    }

    override fun setTitle(title: CharSequence) {
        mToolbar.title = title
        mToolbar.subtitle = ""
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(mNavigationView)) {
            drawer_layout.closeDrawer(mNavigationView)
        }
        else if (navController.currentDestination!!.id == R.id.attendanceListFragment ||
            navController.currentDestination!!.id == R.id.timeTablePagerFragment) {
            ActivityCompat.finishAfterTransition(this)
        } else
            super.onBackPressed()
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
        val name = user.name.split(" ")
                .joinToString(" ") { s -> s.toLowerCase().capitalize() }
        if (user.name.isNotEmpty())
            drawerHeaderVH!!.tvName.text = name
        if (user.course.isNotEmpty())
            drawerHeaderVH!!.tvCourse.text = user.course
    }

    override fun logout() {
        // Remove Sync Account
        MyAccountManager.removeSyncAccount(this)

        // Invalidate the complete network cache
        try {
            httpClient.cache()!!.evictAll()
        } catch (e: IOException) {
            Timber.e(e)
        }

        // Cancel a notification if it is shown.
        val mNotificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(0 /* timetable changed notification id */)

        // Start the Login Authenticator Activity flow
        val ourIntent = Intent(this, AuthenticatorActivity::class.java)
        startActivityForResult(ourIntent, ACTIVITY_RESULT_CODE_AUTHENTICATION)
    }

    companion object {

        /**
         * To prevent saving the drawer position when logging out.
         */
        var LOGGED_OUT = false

        const val ACTIVITY_RESULT_CODE_AUTHENTICATION = 1
    }
}
