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

package com.shalzz.attendance.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.ui.base.BaseActivity
import com.shalzz.attendance.ui.main.MainActivity
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.utils.Miscellaneous.Analytics
import com.shalzz.attendance.wrapper.MySyncManager
import javax.inject.Inject
import javax.inject.Named

class LoginActivity : BaseActivity(), LoginMvpView {

    @BindView(R.id.etUserId)
    lateinit var textInputUserId: TextInputLayout

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    @Inject
    lateinit var mLoginPresenter: LoginPresenter

    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper

    var etUserId: EditText? = null

    var progressDialog: MaterialDialog? = null

    /**
     * Checks if the form is valid
     * @return true or false
     */
    private val isValid: Boolean
        get() {
            val sapid = etUserId!!.text.toString()

            if (sapid.isEmpty() || sapid.length != 10) {
                textInputUserId.requestFocus()
                textInputUserId.error = getString(R.string.form_userid_error)
                Miscellaneous.showKeyboard(this, etUserId)
                return false
            }
            return true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)
        Bugsnag.setContext("LoginActivity")
        mLoginPresenter.attachView(this)

        // set toolbar as actionbar
        setSupportActionBar(mToolbar)

        // Static background with ScrollView
        window.setBackgroundDrawableResource(R.drawable.background)

        etUserId = textInputUserId.editText

        // Shows the CaptchaDialog when user presses 'Done' on keyboard.
        if (etUserId != null) {
            etUserId!!.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin()
                    return@setOnEditorActionListener true
                }
                false
            }
        }
    }

    @OnClick(R.id.bLogin)
    fun doLogin() {
        if (!isValid)
            return

        val userId = etUserId!!.text.toString()
        val bundle = Bundle()
        bundle.putString(Analytics.Param.USER_ID, userId)
        mTracker.logEvent(Analytics.Event.LOGIN_INITIATED, bundle)

        Miscellaneous.closeKeyboard(this, etUserId)
        mLoginPresenter.login(userId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoginPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = MaterialDialog.Builder(this)
                    .content("Logging in...")
                    .cancelable(false)
                    .autoDismiss(false)
                    .progress(true, 0)
                    .build()
        }
        progressDialog!!.show()
    }

    override fun dismissProgressDialog() {
        if (progressDialog != null)
            progressDialog!!.dismiss()
    }

    override fun showMainActivity(user: User) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, "manual")
        mTracker.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

        dismissProgressDialog()
        mPreferencesHelper.saveUser(user.phone.toString())
        MySyncManager.addPeriodicSync(this, user.phone.toString())
        val ourIntent = Intent(this, MainActivity::class.java)
        startActivity(ourIntent)
        finish()
    }

    override fun showError(message: String) {
        dismissProgressDialog()
        Miscellaneous.showSnackBar(mToolbar, message)
    }
}