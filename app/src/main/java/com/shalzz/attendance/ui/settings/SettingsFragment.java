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

package com.shalzz.attendance.ui.settings;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import com.android.billingclient.api.BillingClient;
import com.bugsnag.android.Bugsnag;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.shalzz.attendance.R;
import com.shalzz.attendance.billing.BillingConstants;
import com.shalzz.attendance.billing.BillingProvider;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.event.ProKeyPurchaseEvent;
import com.shalzz.attendance.injection.ActivityContext;
import com.shalzz.attendance.sync.MyAccountManager;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.Miscellaneous.Analytics;
import com.shalzz.attendance.utils.RxEventBus;
import com.shalzz.attendance.utils.RxUtil;
import com.shalzz.attendance.wrapper.ProModeListPreference;

import androidx.preference.SwitchPreferenceCompat;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import javax.inject.Inject;
import javax.inject.Named;

public class SettingsFragment extends PreferenceFragmentCompat implements
        OnSharedPreferenceChangeListener {

    @Inject
    @Named("app")
    FirebaseAnalytics mTracker;

    @Inject
    PreferencesHelper mPreferences;

    @ActivityContext
    @Inject
    Context mContext;

    @Inject
    RxEventBus mEventBus;

    private BillingProvider mBillingProvider;
    private String key_sync_interval;
    private SwitchPreferenceCompat syncPref;
    private SwitchPreferenceCompat proModePref;
    private ProModeListPreference proThemePref;
    private SwitchPreferenceCompat weekendsPref;
    private Activity mActivity;

    private Disposable PurchaseEventDisposable;

    @Override
    public void onStart() {
        super.onStart();
        mTracker.setCurrentScreen(mActivity, getClass().getSimpleName(), getClass().getSimpleName());
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Bugsnag.setContext("Settings");

        addPreferencesFromResource(R.xml.preferences);

        key_sync_interval = getString(R.string.pref_key_sync_interval);
        ListPreference synclistPref = (ListPreference) findPreference(key_sync_interval);
        synclistPref.setSummary(synclistPref.getEntry());

        syncPref = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_sync));
        proThemePref = (ProModeListPreference) findPreference(getString(R.string.pref_key_day_night));
        weekendsPref = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_hide_weekends));
        proModePref = (SwitchPreferenceCompat) findPreference(getString(R.string.pref_key_pro_mode));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mActivity = getActivity();
        ((MainActivity) mActivity).activityComponent().inject(this);

        mBillingProvider = (BillingProvider) mActivity;

        PurchaseEventDisposable = mEventBus.filteredObservable(ProKeyPurchaseEvent.class)
                .subscribe(proKeyPurchaseEvent -> {
                    proModePref.setChecked(true);
                    // Fire an analytics event
                    Bundle params = new Bundle();
                    params.putString(Analytics.Param.USER_ID, mPreferences.getUserId());
                    params.putString(Analytics.Param.IAP_PRODUCT_ID, "prokey");
                    mTracker.logEvent(Analytics.Event.IAP_PURCHASE, params);
                }, Timber::e);
        super.onActivityCreated(savedInstanceState);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getString(R.string.pref_key_day_night))) {
            proThemePref.setSummary(proThemePref.getEntry());
            //noinspection WrongConstant
            AppCompatDelegate.setDefaultNightMode(Integer.parseInt(sharedPreferences.
                    getString(key,"-1")));

            Bundle params = new Bundle();
            params.putString(Analytics.Param.THEME, proThemePref.getEntry().toString());
            mTracker.logEvent(Analytics.Event.THEME_CHANGE, params);
        }
        else if(key.equals(getString(R.string.pref_key_hide_weekends))) {
            if (!mBillingProvider.isProKeyPurchased()) {
                weekendsPref.setChecked(false);
                Toast.makeText(mContext, "Pro key required!", Toast.LENGTH_SHORT).show();
            }
        }
        else if (key.equals(getString(R.string.pref_key_sync))) {
            toggleSync(sharedPreferences.getBoolean(key,true));
        }
        else if(key.equals(key_sync_interval)) {
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
            MyAccountManager.addPeriodicSync(mContext, MyAccountManager.getSyncAccount(mContext));
        }
        else if(key.equals(getString(R.string.pref_key_ga_opt_in))) {
            boolean optIn = sharedPreferences.getBoolean(key, true);
            mTracker.setAnalyticsCollectionEnabled(optIn);
            Timber.i("Opted in to Google Analytics: %b", optIn);
        }

        requestBackup();
    }

    private void toggleSync(boolean sync) {
        MyAccountManager.toggleAutomaticSync(mContext, sync);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxUtil.dispose(PurchaseEventDisposable);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getString(R.string.navigation_item_3));

        syncPref.setChecked(MyAccountManager.isSyncEnabled(mContext));

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        if (mBillingProvider.isProKeyPurchased()) {
            proModePref.setOnPreferenceClickListener(null);
            proModePref.setChecked(true);
            proModePref.setSelectable(false);
        } else {
            proModePref.setChecked(false);
            proModePref.setOnPreferenceClickListener(preference -> {
                proModePref.setChecked(false);
                mBillingProvider.getBillingManager()
                        .initiatePurchaseFlow(BillingConstants.SKU_PRO_KEY, BillingClient.SkuType.INAPP);

                // Fire an analytics event
                Bundle params = new Bundle();
                params.putString(Analytics.Param.USER_ID, mPreferences.getUserId());
                mTracker.logEvent(Analytics.Event.IAP_INITIATED, params);
                return true;
            });
        }

        proThemePref.setProModeListPreferenceClickListener(preference -> {
            if (mBillingProvider.isProKeyPurchased()) {
                proThemePref.showDialog();
            } else {
                Toast.makeText(mContext, "Pro key required!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

    }

    public void requestBackup() {
        BackupManager bm = new BackupManager(mContext);
        bm.dataChanged();
    }
}
