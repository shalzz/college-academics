package com.shalzz.attendance.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.shalzz.attendance.R;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.licenses.License;
import de.psdev.licensesdialog.model.Notice;

public class AboutSettingsFragment extends PreferenceFragment{

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        addPreferencesFromResource(R.xml.pref_about);
    }

    @Override
    public void onResume() {
        super.onResume();

        PreferenceScreen prefScreen =  getPreferenceScreen();
        Preference pref = prefScreen.getPreference(1);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String name = getString(R.string.app_name);
                final String url = getString(R.string.app_url);
                final String copyright = getString(R.string.app_copyright);
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
