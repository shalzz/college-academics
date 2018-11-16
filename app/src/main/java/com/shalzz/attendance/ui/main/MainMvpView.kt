package com.shalzz.attendance.ui.main

import com.shalzz.attendance.billing.BillingManager
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.ui.base.MvpView

/**
 * @author shalzz
 */
interface MainMvpView : MvpView {

    fun getBillingManager(): BillingManager?

    fun updateUserDetails(user: User)

    fun logout()
}
