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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import com.bugsnag.android.Bugsnag;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.R;
import com.shalzz.attendance.injection.ActivityContext;
import com.shalzz.attendance.ui.main.MainActivity;

import javax.inject.Inject;
import javax.inject.Named;

public class AboutSettingsFragment extends PreferenceFragmentCompat {

    private MainActivity mainActivity;

    @Inject @Named("app")
    FirebaseAnalytics mTracker;

    @ActivityContext
    @Inject
    Context mContext;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Bugsnag.setContext("About");
        addPreferencesFromResource(R.xml.pref_about);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mainActivity = ((MainActivity) getActivity());
        mainActivity.activityComponent().inject(this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        mTracker.setCurrentScreen(mainActivity, getClass().getSimpleName(), getClass().getSimpleName());
    }

    @Override
    public void onResume() {
        super.onResume();
        int index = 0;

        PreferenceScreen prefScreen =  getPreferenceScreen();
        Preference auth = prefScreen.getPreference(index++);

        auth.setSummary(getString(R.string.copyright_year)+ " "
                + getString(R.string.app_copyright));
	    auth.setOnPreferenceClickListener(preference -> {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, getString(R.string.pref_author));
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Author");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "preference");
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            return true;
        });

        Preference noticePref = prefScreen.getPreference(index++);
        noticePref.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(mContext, OssLicensesMenuActivity.class));

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, getString(R.string.pref_key_info_notices));
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "OSS License");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "preference");
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            return true;
        });

        Preference versionPref = prefScreen.getPreference(index++);
        versionPref.setSummary(BuildConfig.VERSION_NAME);
        versionPref.setOnPreferenceClickListener(preference -> {

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, getString(R.string.pref_key_info_version));
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Version");
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "preference");
            mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            return true;
        });
    }
}
