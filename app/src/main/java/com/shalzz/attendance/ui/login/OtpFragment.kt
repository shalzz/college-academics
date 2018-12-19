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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.shalzz.attendance.R
import com.shalzz.attendance.utils.Miscellaneous
import kotlinx.android.synthetic.main.fragment_otp.*
import kotlinx.android.synthetic.main.fragment_otp.view.*
import javax.inject.Inject
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OTPFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OTPFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class OTPFragment : Fragment(), OtpMvpView {
    private var phone: String? = null
    private var sender: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private var progressDialog: MaterialDialog? = null
    private lateinit var mActivity: Activity

    @Inject
    lateinit var mOtpPresenter: OtpPresenter

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action!!.equals("otp", ignoreCase = true)) {
                val message = intent.getStringExtra("code")
                etOTP.editText!!.setText(message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            phone = it.getString(ARG_PHONE)
            sender = it.getString(ARG_SENDER)
        }
        mActivity = activity as Activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val mView = inflater.inflate(R.layout.fragment_otp, container, false)

        (mActivity as AuthenticatorActivity).activityComponent().inject(this)
        Bugsnag.setContext("OTP Fragment")
        mOtpPresenter.attachView(this)

        // Attempt to login when user presses 'Done' on keyboard.
        mView.etOTP!!.editText!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyOTP()
                return@setOnEditorActionListener true
            }
            false
        }
        mView.button.setOnClickListener {
            verifyOTP()
        }

        requestSmsPermission()
        mView.smsPermission.setOnClickListener { requestSmsPermission() }
        return mView
    }

    private fun requestSmsPermission() {
        val permission = Manifest.permission.RECEIVE_SMS
        val grant = ContextCompat.checkSelfPermission(mActivity, permission)
        if (grant != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, arrayOf(permission), 1)
        }
    }

    private fun verifyOTP() {
        val otp : String = etOTP.editText!!.text.toString()
        if (otp.isEmpty() || otp.length !in 4..6  ) {
            etOTP.requestFocus()
            etOTP.error = getString(R.string.form_otp_error)
            Miscellaneous.showKeyboard(mActivity, etOTP.editText)
        } else {
            mOtpPresenter.verifyOTP(phone!!, Integer.parseInt(otp))
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

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
        fun onFragmentInteraction(authToken: String, phone: String)
    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(mActivity)
                .registerReceiver(receiver, IntentFilter("otp"))
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(receiver)
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

    override fun successfulLogin(authToken: String) {
        listener?.onFragmentInteraction(authToken, phone!!)
    }

    override fun showError(message: String?) {
        dismissProgressDialog()

        Miscellaneous.showSnackBar(etOTP,
            message ?: getString(R.string.unexpected_error)
        )
    }

    companion object {
        const val ARG_PHONE = "phone"
        const val ARG_SENDER = "sender"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param phone the phone number.
         * @param sender the sendername of the otp msg.
         * @return A new instance of fragment OTPFragment.
         */
        @JvmStatic
        fun newInstance(phone: String, sender: String) =
                OTPFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PHONE, phone)
                        putString(ARG_SENDER, sender)
                    }
                }
    }
}
