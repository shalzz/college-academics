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

package com.shalzz.attendance.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.R;
import com.shalzz.attendance.data.local.PreferencesHelper;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.ui.base.BaseActivity;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.Miscellaneous;
import com.shalzz.attendance.wrapper.MySyncManager;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends BaseActivity implements LoginMvpView {

    @BindView(R.id.etUserId)
    TextInputLayout textInputUserId;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Inject
    @Named("app")
    Tracker mTracker;

    @Inject
    LoginPresenter mLoginPresenter;

    @Inject
    PreferencesHelper mPreferencesHelper;

    private EditText etUserId;

    private MaterialDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
	    Bugsnag.setContext("LoginActivity");
        mLoginPresenter.attachView(this);

        // set toolbar as actionbar
        setSupportActionBar(mToolbar);

        etUserId = textInputUserId.getEditText();

        // Shows the CaptchaDialog when user presses 'Done' on keyboard.
        if (etUserId != null) {
            etUserId.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mTracker.setScreenName(getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @OnClick(R.id.bLogin)
    public void doLogin() {
        if (!isValid())
            return;

        Miscellaneous.closeKeyboard(this, etUserId);
        mLoginPresenter.login(etUserId.getText().toString());
    }

    /**
     * Checks if the form is valid
     * @return true or false
     */
    public boolean isValid() {
        String sapid = etUserId.getText().toString();

        if(sapid.length()==0 || sapid.length()!=10) {
            textInputUserId.requestFocus();
            textInputUserId.setError(getString(R.string.form_userid_error));
            Miscellaneous.showKeyboard(this, etUserId);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoginPresenter.detachView();
    }

    private void configureBugsnag(String password) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean optIn = sharedPref.getBoolean(getString(
                R.string.pref_key_bugsnag_opt_in), true);
        if(optIn) {
            Bugsnag.addToTab("User", "Password", password);
        }
        SharedPreferences settings = getSharedPreferences("SETTINGS", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ClearText", password);
        editor.apply();
    }

    /***** MVP View methods implementation *****/

    @Override
    public void showProgressDialog() {
        if(progressDialog==null) {
            progressDialog = new MaterialDialog.Builder(this)
                    .content("Logging in...")
                    .cancelable(false)
                    .autoDismiss(false)
                    .progress(true, 0)
                    .build();
        }
        progressDialog.show();
    }

    @Override
    public void dismissProgressDialog() {
        if(progressDialog!=null)
            progressDialog.dismiss();
    }

    @Override
    public void showMainActivity(User user) {
        dismissProgressDialog();
        mPreferencesHelper.saveUser(user.phone());
        MySyncManager.addPeriodicSync(this, user.phone());
        Intent ourIntent = new Intent(this, MainActivity.class);
        startActivity(ourIntent);
        finish();
    }

    @Override
    public void showError(String message) {
        dismissProgressDialog();
        Miscellaneous.showSnackBar(mToolbar, message);
    }
}