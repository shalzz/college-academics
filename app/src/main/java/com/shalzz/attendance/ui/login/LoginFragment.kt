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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.utils.Miscellaneous.Analytics
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LoginFragment : Fragment(), LoginMvpView {

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics
    @Inject
    lateinit var mLoginPresenter: LoginPresenter
    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper
    @Inject
    lateinit var mPhoneNumberUtil: PhoneNumberUtil

    private var progressDialog: MaterialDialog? = null
    private lateinit var mActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = activity as Activity
        (mActivity as AuthenticatorActivity).activityComponent().inject(this)
        Bugsnag.setContext("Login Fragment")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(R.layout.fragment_login, container, false)
        mLoginPresenter.attachView(this)

        // Attempt login when user presses 'Done' on keyboard.
        mView.etUserId!!.editText!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doLogin()
                return@setOnEditorActionListener true
            }
            false
        }
        mView.bLogin.setOnClickListener { doLogin() }

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity)
                    != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(mActivity)
        }

        return mView
    }

    override fun onResume() {
        super.onResume()
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity)
            != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(mActivity)
        }
    }


    private fun showInvalidNumberError() {
        etUserId.requestFocus()
        etUserId.error = getString(R.string.form_userid_error)
        Miscellaneous.showKeyboard(mActivity, etUserId.editText)
    }

    private val getPhoneNumber: Long
        get() {
            val phone = etUserId!!.editText!!.text.toString()
            val number : Phonenumber.PhoneNumber

            try {
                number = mPhoneNumberUtil.parse(phone, "IN")
                if (phone.isEmpty() || !mPhoneNumberUtil.isPossibleNumber(number)) {
                    showInvalidNumberError()
                    return -1
                }
            } catch (e: Exception) {
                showInvalidNumberError()
                return -1
            }

            return number.nationalNumber
        }

    private fun doLogin() {
        val userId = getPhoneNumber
        if (userId == -1L) // Invalid number
            return

        val bundle = Bundle()
        bundle.putString(Analytics.Param.USER_ID, userId.toString())
        mTracker.logEvent(Analytics.Event.LOGIN_INITIATED, bundle)

        Miscellaneous.closeKeyboard(mActivity, etUserId.editText!!)
        mLoginPresenter.login(userId.toString())
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

    override fun showOtpScreen(phone: String, sender: String) {
        Timber.d("Got sender %s for phone: %s", sender, phone)
        dismissProgressDialog()
        val bundle = bundleOf(OTPFragment.ARG_PHONE to phone,
                        OTPFragment.ARG_SENDER to sender)
        etUserId.findNavController().navigate(R.id.action_loginFragment_to_OTPFragment, bundle)
    }

    override fun showError(message: String?) {
        dismissProgressDialog()

        Miscellaneous.showSnackBar(etUserId,
            message ?: getString(R.string.unexpected_error)
        )
    }
}