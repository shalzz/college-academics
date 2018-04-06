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
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.MyApplication;
import com.shalzz.attendance.R;
import com.shalzz.attendance.ui.main.MainActivity;

import javax.inject.Inject;
import javax.inject.Named;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.licenses.License;
import de.psdev.licensesdialog.model.Notice;

public class AboutSettingsFragment extends PreferenceFragmentCompat {

    private Context mContext;
    private MainActivity mainActivity;

    @Inject @Named("app")
    Tracker mTracker;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mContext = getActivity();
        MyApplication.get(mContext).getComponent().inject(this);
        Bugsnag.setContext("About");
        mainActivity = ((MainActivity) getActivity());
        mainActivity.setDrawerAsUp(true);

        addPreferencesFromResource(R.xml.pref_about);
    }

    @Override
    public void onStart() {
        super.onStart();

        mTracker.setScreenName(getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.setTitle(getString(R.string.about_settings_title));

        PreferenceScreen prefScreen =  getPreferenceScreen();
        Preference auth = prefScreen.getPreference(0);

        auth.setSummary(getString(R.string.copyright_year)+ " "
                + getString(R.string.app_copyright));
	    auth.setOnPreferenceClickListener(preference -> {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Click")
                    .setAction("Author")
                    .build());
            return true;
        });

        Preference pref = prefScreen.getPreference(1);
        pref.setOnPreferenceClickListener(preference -> {
            final String name = getString(R.string.app_name);
            final String url = getString(R.string.app_url);
            final String copyright = getString(R.string.copyright_year) + " "
                                        + getString(R.string.app_copyright);
            final License license = new GnuGeneralPublicLicense20();
            final Notice notice = new Notice(name, url, copyright, license);
            new LicensesDialog.Builder(mContext)
                    .setNotices(notice)
                    .setShowFullLicenseText(true)
                    .build()
                    .show();

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Click")
                    .setAction("License")
                    .build());
            return true;
        });

        Preference noticePref = prefScreen.getPreference(2);
        noticePref.setOnPreferenceClickListener(preference -> {
            new LicensesDialog.Builder(mContext)
                    .setNotices(R.raw.notices)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Click")
                    .setAction("OSS License")
                    .build());
            return true;
        });

        Preference versionPref = prefScreen.getPreference(3);
        versionPref.setSummary(BuildConfig.VERSION_NAME);
        versionPref.setOnPreferenceClickListener(preference -> {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Click")
                    .setAction("Version")
                    .build());
            return true;
        });
    }
}
