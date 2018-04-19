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

package com.shalzz.attendance.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.Purchase;
import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.R;
import com.shalzz.attendance.billing.BillingConstants;
import com.shalzz.attendance.billing.BillingManager.BillingUpdatesListener;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.event.ProKeyPurchaseEvent;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.ui.base.BasePresenter;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.utils.RxEventBus;
import com.shalzz.attendance.utils.RxUtil;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ConfigPersistent
public class MainPresenter extends BasePresenter<MainMvpView> {

    private DataManager mDataManager;
    private PreferencesHelper mPreferenceHelper;

    private Disposable mDisposable;
    private Context mContext;
    private final UpdateListener mUpdateListener;

    // Tracks if we currently own a pro key
    private boolean mIsProUnlocked;

    @Inject
    @Named("app")
    Tracker mTracker;

    @Inject
    RxEventBus mEventBus;

    @Inject
    MainPresenter(DataManager dataManager,
                  PreferencesHelper preferencesHelper,
                  @ApplicationContext Context context) {
        mDataManager = dataManager;
        mPreferenceHelper = preferencesHelper;
        mContext = context;
        mUpdateListener = new UpdateListener();
    }

    @Override
    public void attachView(MainMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        RxUtil.dispose(mDisposable);
    }

    public void loadUser() {
        RxUtil.dispose(mDisposable);
        mDisposable = mDataManager.loadUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<User>() {
                    @Override
                    public void onNext(User user) {
                        if (isViewAttached()) {
                            getMvpView().updateUserDetails(user);
                        }
                        Bugsnag.setUserId(user.id());

                        SharedPreferences sharedPref =
                                PreferenceManager.getDefaultSharedPreferences(mContext);
                        boolean optIn = sharedPref.getBoolean(mContext.getString(
                                R.string.pref_key_bugsnag_opt_in), true);
                        if(optIn) {
                            Bugsnag.setUserName(user.name());
                            Bugsnag.addToTab("User", "phone", user.phone());
                            Bugsnag.addToTab("User", "email", user.email());
                        }

                        mTracker.set("&uid", user.phone());
                        mTracker.send(new HitBuilders.ScreenViewBuilder()
                                .setCustomDimension(Miscellaneous.CUSTOM_DIMENSION_USER_ID,
                                        user.phone())
                                .build());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void logout() {
        checkViewAttached();
        MainActivity.LOGGED_OUT = true;

        // Remove User Details from Shared Preferences.
        mPreferenceHelper.removeUser();

        // Remove user Attendance data from database.
        mDataManager.resetTables();

        getMvpView().logout();
    }

    public UpdateListener getUpdateListener() {
        return mUpdateListener;
    }

    public boolean isProKeyPurchased() {
        return mIsProUnlocked;
    }

    /**
     * Handler to billing updates
     */
    private class UpdateListener implements BillingUpdatesListener {

        @Override
        public void onBillingClientSetupFinished() {
            if(!isViewAttached())
                return;
            int billingResponseCode = getMvpView().getBillingManager()
                    .getBillingClientResponseCode();

            Timber.i("Billing response: %d", billingResponseCode);
            switch (billingResponseCode) {
                case BillingResponse.OK:
                    // If manager was connected successfully, do nothing
                    Timber.i("Billing response2: %d", billingResponseCode);
                    break;
                case BillingResponse.BILLING_UNAVAILABLE:
                    Toast.makeText(mContext, R.string.error_billing_unavailable, Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(mContext, R.string.error_billing_default, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onConsumeFinished(String token, int result) {


        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {
            for (Purchase purchase : purchaseList) {
                switch (purchase.getSku()) {
                    case BillingConstants.SKU_PRO_KEY:
                        Timber.d("You are Premium! Congratulations!!!");
                        mIsProUnlocked = true;
                        mEventBus.post(new ProKeyPurchaseEvent());
                        break;
                }
            }
        }
    }
}
