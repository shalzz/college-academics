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

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
    private Toolbar mToolbar;

    private int mContentViewHeight;
    private String myTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        // set toolbar as actionbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if(getIntent().hasExtra(SplashActivity.INTENT_EXTRA_STARTING_ACTIVITY)) {
            mToolbar.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mToolbar.getViewTreeObserver().removeOnPreDrawListener(this);
                            final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                            final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

                            mToolbar.measure(widthSpec, heightSpec);
                            mContentViewHeight = mToolbar.getHeight();
                            collapseToolbar();
                            return true;
                        }
                    });
        } else {
            int toolBarHeight;
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            toolBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, getResources().getDisplayMetrics());
            ViewGroup.LayoutParams lp = mToolbar.getLayoutParams();
            lp.height = toolBarHeight;
            mToolbar.setLayoutParams(lp);
        }
        setSupportActionBar(mToolbar);
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

    private void collapseToolbar() {
        int toolBarHeight;
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        toolBarHeight = TypedValue.complexToDimensionPixelSize(
                tv.data, getResources().getDisplayMetrics());

        ValueAnimator valueHeightAnimator = ValueAnimator
                .ofInt(mContentViewHeight, toolBarHeight);

        valueHeightAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        ViewGroup.LayoutParams lp = mToolbar.getLayoutParams();
                        lp.height = (Integer) animation.getAnimatedValue();
                        mToolbar.setLayoutParams(lp);
                    }
                });

        valueHeightAnimator.start();
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
        MyVolley.getInstance().cancelPendingRequests(MyVolley.APPLICATION_NETWORK_TAG);
        super.onDestroy();
    }
}