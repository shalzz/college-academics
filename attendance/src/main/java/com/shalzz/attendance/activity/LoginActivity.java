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

package com.shalzz.attendance.activity;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bugsnag.android.Bugsnag;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.controllers.UserAccount;
import com.shalzz.attendance.wrapper.MyVolley;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class LoginActivity extends AppCompatActivity {

    @InjectView(R.id.etSapid) TextInputLayout textInputSapid;
    @InjectView(R.id.etPass) TextInputLayout textInputPass;
    @SuppressWarnings("FieldCanBeLocal")
    @InjectView(R.id.bLogin) Button bLogin;
    private EditText etSapid;
    private EditText etPass;

    private String myTag ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);
        Bugsnag.setContext("LoginActivity");

        // set toolbar as actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        myTag = getLocalClassName();

        etSapid = textInputSapid.getEditText();
        etPass = textInputPass.getEditText();

        // Shows the CaptchaDialog when user presses 'Done' on keyboard.
        etPass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (isValid()) {
                        Login();
                    }
                    return true;
                }
                return false;
            }
        });

        // OnClickListener event for the Login Button
        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValid())
                    Login();
            }
        });
    }

    public void Login() {

        Miscellaneous.closeKeyboard(this, etPass);
        new UserAccount(LoginActivity.this)
                .Login(etSapid.getText().toString(), etPass.getText().toString());
    }

    /**
     * Checks if the form is valid
     * @return true or false
     */
    public boolean isValid() {
        String sapid = etSapid.getText().toString();
        String password = etPass.getText().toString();

        if(sapid.length()==0 || sapid.length()!=9) {
            etSapid.requestFocus();
            etSapid.setError(getString(R.string.form_sapid_error));
            Miscellaneous.showKeyboard(this, etSapid);
            return false;
        }
        else if (password.length()==0) {
            etPass.requestFocus();
            etPass.setError(getString(R.string.form_password_error));
            Miscellaneous.showKeyboard(this,etPass);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        MyVolley.getInstance().cancelPendingRequests(MyVolley.APPLICATION_NETWORK_TAG);
        super.onDestroy();
    }
}