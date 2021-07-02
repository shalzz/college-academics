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

import android.content.Context
import android.preference.PreferenceManager
import android.widget.Toast
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.bugsnag.android.Bugsnag
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.billing.BillingConstants
import com.shalzz.attendance.billing.BillingManager.BillingUpdatesListener
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.LogoutModel
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.event.ProKeyPurchaseEvent
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.RxEventBus
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ConfigPersistent
class MainPresenter @Inject
internal constructor(private val mDataManager: DataManager,
                     private val mPreferenceHelper: PreferencesHelper,
                     @param:ApplicationContext private val mContext: Context) : BasePresenter<MainMvpView>() {

    private var mDisposable: Disposable? = null
    private var mSyncDisposable: Disposable? = null
    val updateListener: UpdateListener

    // Tracks if we currently own a pro key
    var isProKeyPurchased: Boolean = false
        private set

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    @Inject
    lateinit var mEventBus: RxEventBus

    init {
        updateListener = UpdateListener()
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    fun loadUser(username: String) {
        RxUtil.dispose(mDisposable)
        mDisposable = mDataManager.loadUser(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<User>() {
                    override fun onNext(user: User) {
                        if (isViewAttached) {
                            mvpView.updateUserDetails(user)
                        }
                        Bugsnag.setUser(user.username, user.email, user.name)

                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
                        val optIn = sharedPref.getBoolean(mContext.getString(
                                R.string.pref_key_ga_opt_in), true)
                        if (optIn) {
                            Bugsnag.setUser(user.username, user.college, user.name)
                        }

                        mTracker.setUserId(user.username)
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun onComplete() {

                    }
                })
    }

    fun logout() {
        RxUtil.dispose(mDisposable)
        mDisposable = mDataManager.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<LogoutModel>() {
                    override fun onNext(result: LogoutModel) {
                        // Remove User Details from Shared Preferences.
                        mPreferenceHelper.removeUser()

                        // Remove user Attendance data from database.
                        mDataManager.resetTables()

                        MainActivity.LOGGED_OUT = true
                    }

                    override fun onError(e: Throwable) {
                        Timber.e(e)
                    }

                    override fun onComplete() {
                        if (isViewAttached) {
                            mvpView.logout()
                        }
                    }
                })
    }

    /**
     * Handler to billing updates
     */
    inner class UpdateListener : BillingUpdatesListener {
        override fun onBillingClientSetupFinished(result: BillingResult?) {
            Timber.i("Billing response: %d", result?.responseCode)
            when (result?.responseCode) {
                BillingResponseCode.OK ->
                    // If manager was connected successfully, do nothing
                    Timber.i("Billing response2: %d", result?.responseCode)
                BillingResponseCode.BILLING_UNAVAILABLE ->
                    Toast.makeText(mContext, R.string.error_billing_unavailable, Toast.LENGTH_LONG)
                        .show()
                else -> Toast.makeText(mContext, R.string.error_billing_default, Toast.LENGTH_LONG)
                    .show()
            }
        }

        override fun onConsumeFinished(token: String, result: Int) {

        }

        override fun onPurchasesUpdated(purchaseList: List<Purchase>) {
            for (purchase in purchaseList) {
                for (sku in purchase.skus) {
                    when (sku) {
                        BillingConstants.SKU_PRO_KEY -> {
                            Timber.d("You are Premium! Congratulations!!!")
                            isProKeyPurchased = true
                            mEventBus.post(ProKeyPurchaseEvent())
                        }
                    }
                }
            }
        }
    }
}
