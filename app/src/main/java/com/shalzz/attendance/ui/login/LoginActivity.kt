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

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.utils.Miscellaneous.Analytics
import javax.inject.Inject
import javax.inject.Named

class LoginFragment : Fragment(), LoginMvpView {

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

    private lateinit var unbinder: Unbinder
    private lateinit var mActivity: Activity

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
                Miscellaneous.showKeyboard(mActivity, etUserId)
                return false
            }
            return true
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(R.layout.activity_login, container, false)
        unbinder = ButterKnife.bind(this, mView)

        mActivity = activity as Activity
        (mActivity as AuthenticatorActivity).activityComponent().inject(this)
        Bugsnag.setContext("LoginActivity")
        mLoginPresenter.attachView(this)

        // set toolbar as actionbar
        mActivity.setSupportActionBar(mToolbar)

        // Static background with ScrollView
        mActivity.window.setBackgroundDrawableResource(R.drawable.background)

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

        Miscellaneous.closeKeyboard(mActivity, etUserId)
        mLoginPresenter.login(userId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoginPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = MaterialDialog.Builder(mActivity)
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

    override fun successfulLogin(authtoken: String) {
        val data = Bundle()
//        data.putString(AccountManager.KEY_ACCOUNT_NAME, userName)
//        data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
//        data.putString(AccountManager.KEY_AUTHTOKEN, authtoken)
//        data.putString(PARAM_USER_PASS, userPass)
        val res = Intent()
        res.putExtras(data)
        if (res.hasExtra(AccountManager.KEY_ERROR_MESSAGE)) {
            Toast.makeText(mActivity, res.getStringExtra(AccountManager.KEY_ERROR_MESSAGE), Toast
                    .LENGTH_SHORT).show()
        } else {
            finishLogin(res)
        }
    }

    override fun showError(message: String?) {
        dismissProgressDialog()

        Miscellaneous.showSnackBar(mToolbar,
                if (message == null) message
                else getString(R.string.unexpected_error)
        )
    }
}