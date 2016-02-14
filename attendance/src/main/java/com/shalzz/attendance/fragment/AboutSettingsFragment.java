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

package com.shalzz.attendance.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.R;
import com.shalzz.attendance.wrapper.MyVolley;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.licenses.License;
import de.psdev.licensesdialog.model.Notice;

public class AboutSettingsFragment extends PreferenceFragmentCompat {

    private Context mContext;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mContext = getActivity();

        addPreferencesFromResource(R.xml.pref_about);
    }

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferenceScreen prefScreen =  getPreferenceScreen();
        Preference auth = prefScreen.getPreference(0);
        char x[] = {83,117,100,105,116,105,105,32,83,105,110,103,104};
        int trans[] =   {0,-13,-3,3,-15,-4,5,0,-9,-8,-5,7,-72};
        for (int i = 0; i < x.length; ++i) {
            x[i] = (char) (x[i] + trans[i]);
        }
        auth.setSummary(getString(R.string.copyright_year)+ " " +String.valueOf(x));
        Preference pref = prefScreen.getPreference(1);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String name = getString(R.string.app_name);
                final String url = getString(R.string.app_url);
                final String copyright = getString(R.string.copyright_year) + " "
                                            + getString(R.string.app_copyright);
                final License license = new GnuGeneralPublicLicense20();
                final Notice notice = new Notice(name, url, copyright, license);
                new LicensesDialog.Builder(mContext).setNotices(notice).setShowFullLicenseText(true).build().show();
                return true;
            }
        });
        Preference noticePref = prefScreen.getPreference(2);
        noticePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new LicensesDialog.Builder(mContext).setNotices(R.raw.notices).setIncludeOwnLicense(true)
                        .build().show();
                return true;
            }
        });
    }

}
