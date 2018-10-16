package com.shalzz.attendance.ui.main;

import com.shalzz.attendance.billing.BillingManager;
import com.shalzz.attendance.data.model.entity.User;
import com.shalzz.attendance.ui.base.MvpView;

/**
 * @author shalzz
 */
public interface MainMvpView extends MvpView {

    BillingManager getBillingManager();

    void updateUserDetails(User user);

    void logout();
}
