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

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.wrapper.MySyncManager;
import com.shalzz.attendance.wrapper.MyVolley;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{

	private Context mContext;
    private String key_sub_limit;
    private String key_sync_interval;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();

		addPreferencesFromResource(R.xml.preferences);

        key_sub_limit = getString(R.string.pref_key_sub_limit);
		ListPreference listPref = (ListPreference) findPreference(key_sub_limit);
		listPref.setSummary(listPref.getEntry());

        key_sync_interval = getString(R.string.pref_key_sync_interval);
		ListPreference synclistPref = (ListPreference) findPreference(key_sync_interval);
		synclistPref.setSummary(synclistPref.getEntry());
	}

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(key_sub_limit)) {
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
        }
        else if (key.equals(getString(R.string.pref_key_sync))) {
            DatabaseHandler db = new DatabaseHandler(mContext);
            String account_name =  "" + db.getUser().getSapid();
            if (sharedPreferences.getBoolean(key,true))
                MySyncManager.enableAutomaticSync(mContext, account_name);
            else
                MySyncManager.disableAutomaticSync(mContext, account_name);
        }
        else if(key.equals(key_sync_interval)) {
            DatabaseHandler db = new DatabaseHandler(mContext);
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
            MySyncManager.addPeriodicSync(mContext, "" + db.getUser().getSapid());
        }
        else if(key.equals(getString(R.string.pref_key_ga_opt_in))) {
            GoogleAnalytics.getInstance(mContext).setAppOptOut(
                    !sharedPreferences.getBoolean(key, false));
        }
	}

	@Override
	public void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
		.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
		.registerOnSharedPreferenceChangeListener(this);

		PreferenceCategory prefCategory = (PreferenceCategory) getPreferenceScreen()
                .getPreference(2);
		PreferenceScreen prefScreen =  (PreferenceScreen) prefCategory.getPreference(0);
        prefScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference preference) {
                Fragment mFragment = new AboutSettingsFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.frame_container, mFragment, MainActivity.FRAGMENT_TAG);
                transaction.addToBackStack(null);
                ((MainActivity)getActivity()).mPopSettingsBackStack = true;

                transaction.commit();
				return true;
			}
		});

        PreferenceCategory proxyPrefCategory = (PreferenceCategory) getPreferenceScreen()
                .getPreference(1);
        PreferenceScreen proxyPrefScreen =  (PreferenceScreen) proxyPrefCategory.getPreference(3);
        proxyPrefScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Fragment mFragment = new ProxySettingsFragment();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();

                transaction.replace(R.id.frame_container, mFragment, MainActivity.FRAGMENT_TAG);
                transaction.addToBackStack(null);
                ((MainActivity)getActivity()).mPopSettingsBackStack = true;

                transaction.commit();
                return true;
            }
        });
	}
}