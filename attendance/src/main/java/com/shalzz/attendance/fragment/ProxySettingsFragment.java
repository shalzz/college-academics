package com.shalzz.attendance.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.R;
import com.shalzz.attendance.wrapper.MyVolley;
import com.squareup.leakcanary.RefWatcher;

public class ProxySettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String key_proxy_username;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context mContext = getActivity();

        addPreferencesFromResource(R.xml.pref_proxy);

        key_proxy_username = getString(R.string.pref_key_proxy_username);
        Preference connectionPref = findPreference(key_proxy_username);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        connectionPref.setSummary(sharedPref.getString(key_proxy_username, ""));
    }

    @Override
    public void onStart() {
        super.onStart();
        Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
                MyVolley.TrackerName.APP_TRACKER);

        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(key_proxy_username)) {
            Preference connectionPref = findPreference(key);
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = MyVolley.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
