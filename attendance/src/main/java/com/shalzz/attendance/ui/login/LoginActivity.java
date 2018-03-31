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

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.R;
import com.shalzz.attendance.ui.base.BaseActivity;
import com.shalzz.attendance.utils.Miscellaneous;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LoginActivity extends BaseActivity implements LoginMvpView {

    @BindView(R.id.etSapid)
    TextInputLayout textInputSapid;

    @BindView(R.id.etPass)
    TextInputLayout textInputPass;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @Inject
    UserAccount mUserAccount;

    @Inject
    LoginPresenter mPresenter;

    private EditText etSapid;
    private EditText etPass;

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
        mPresenter.attachView(this);

        // set toolbar as actionbar
        setSupportActionBar(mToolbar);

        etSapid = textInputSapid.getEditText();
        etPass = textInputPass.getEditText();

        // Shows the CaptchaDialog when user presses 'Done' on keyboard.
        if (etPass != null) {
            etPass.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (isValid()) {
                        Login();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    @OnClick(R.id.bLogin)
    public void Login() {
        if (!isValid())
            return;

        Miscellaneous.closeKeyboard(this, etPass);
        mUserAccount.Login(etSapid.getText().toString(), etPass.getText().toString());
    }

    /**
     * Checks if the form is valid
     * @return true or false
     */
    public boolean isValid() {
        String sapid = etSapid.getText().toString();
        String password = etPass.getText().toString();

        if(sapid.length()==0 || sapid.length()!=9) {
            textInputSapid.requestFocus();
            textInputSapid.setError(getString(R.string.form_sapid_error));
            Miscellaneous.showKeyboard(this, etSapid);
            return false;
        }
        else if (password.length()==0) {
            textInputPass.requestFocus();
            textInputPass.setError(getString(R.string.form_password_error));
            Miscellaneous.showKeyboard(this,etPass);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUserAccount = null;
        mPresenter.detachView();
    }
}