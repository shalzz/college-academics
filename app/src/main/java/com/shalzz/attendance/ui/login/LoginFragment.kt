/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.College
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.utils.Miscellaneous.Analytics
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.view.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LoginFragment : Fragment(), LoginMvpView, AdapterView.OnItemSelectedListener,
        CaptchaDialogFragment.CaptchaDialogListener {

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics
    @Inject
    lateinit var mLoginPresenter: LoginPresenter
    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper

    private var progressDialog: MaterialDialog? = null
    private lateinit var mActivity: Activity
    private var listener: LoginFragment.OnFragmentInteractionListener? = null
    private lateinit var spinnerAdapter: ArrayAdapter<College>
    private var college: College? = null

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(authToken: String, username: String, password: String)
    }

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

        spinnerAdapter = ArrayAdapter(mActivity, android.R.layout.simple_list_item_1,
                ArrayList<College>())
        mView.spCollege.adapter = spinnerAdapter

        mView.spCollege.onItemSelectedListener = this

        // Attempt login when user presses 'Done' on keyboard.
        mView.etPassword!!.editText!!.setOnEditorActionListener { _, actionId, _ ->
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

        mLoginPresenter.loadColleges()

        return mView
    }

    override fun onResume() {
        super.onResume()
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity)
            != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(mActivity)
        }
    }


    private fun isValid(username: String, password: String): Boolean {
        var valid = true
        etUserId.error = null
        etPassword.error = null
        if (college == null) { // TODO
            dismissProgressDialog()
            valid = false
        }
        if (username.isEmpty()) {
            etUserId.requestFocus()
            etUserId.error = getString(R.string.form_userid_error)
            Miscellaneous.showKeyboard(mActivity, etUserId.editText)
            dismissProgressDialog()
            valid = false
        }
        if (password.isEmpty()) {
            etPassword.requestFocus()
            etPassword.error = getString(R.string.form_password_error)
            Miscellaneous.showKeyboard(mActivity, etPassword.editText)
            dismissProgressDialog()
            valid = false
        }

        return valid
    }

    private fun doLogin() {
        val userId = etUserId.editText!!.editableText
        val password = etPassword.editText!!.editableText

        if (!isValid(userId.toString(), password.toString())) {
            return
        }

        val bundle = Bundle()
        bundle.putString(Analytics.Param.USER_ID, userId.toString())
        bundle.putString(Analytics.Param.PASSWORD, password.toString())
        bundle.putString(Analytics.Param.COLLEGE, college!!.id)
        mTracker.logEvent(Analytics.Event.LOGIN_INITIATED, bundle)

        Timber.d("new login: %s, %s, %s", userId, password, college!!.id)

        Miscellaneous.closeKeyboard(mActivity, etPassword.editText)
        mLoginPresenter.login(userId.toString(), password.toString(), college!!.id)
    }

    override fun onDialogPositiveClick(captcha: String) {
        Timber.d("Got Captcha: %s", captcha)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LoginFragment.OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        college = null
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        college = spinnerAdapter.getItem(position)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoginPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun showProgressDialog(msg: String) {
        progressDialog = MaterialDialog.Builder(mActivity)
                .content(msg)
                .cancelable(false)
                .autoDismiss(false)
                .progress(true, 0)
                .build()
        progressDialog!!.show()
    }

    override fun dismissProgressDialog() {
        if (progressDialog != null)
            progressDialog!!.dismiss()
    }

    override fun showCaptchaDialog() {
        dismissProgressDialog()
        val dialog = CaptchaDialogFragment(this)
        dialog.show(fragmentManager, "captcha-dialog")
    }

    override fun updateCollegeList(data: List<College>) {
        spinnerAdapter.clear()
        spinnerAdapter.addAll(data)
        Timber.d("Colleges: %s", data)
    }

    override fun saveToken(username: String, college: String, authToken: String) {
        val token = Base64.encodeToString("$username:$authToken".toByteArray(), Base64.NO_WRAP)
        mPreferencesHelper.saveToken(token)
        mPreferencesHelper.saveCollege(college)
    }

    override fun showMainActivity(user: User, password: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.METHOD, "manual")
        bundle.putString(Analytics.Param.USER_ID, user.username)
        bundle.putString(Analytics.Param.COLLEGE, user.college)
        mTracker.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)

        dismissProgressDialog()
        val token = mPreferencesHelper.token!!
        mPreferencesHelper.saveUser(user.username, password, user.college)
        listener?.onFragmentInteraction(token, user.username, password)
        mPreferencesHelper.setLoggedIn()
    }

    override fun showError(message: String?) {
        dismissProgressDialog()

        Miscellaneous.showSnackBar(etUserId,
            message ?: getString(R.string.unexpected_error)
        )
    }
}