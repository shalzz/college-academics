package com.shalzz.attendance.ui.login

import com.shalzz.attendance.ui.base.MvpView

/**
 * @author shalzz
 */
interface OtpMvpView : MvpView {

    fun successfulLogin(authToken: String)

    fun showError(message: String?)

    fun showProgressDialog()

    fun dismissProgressDialog()
}