package com.shalzz.attendance.ui.main;

import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.ui.base.MvpView;

/**
 * @author shalzz
 */
public interface MainMvpView extends MvpView {

    void updateUserDetails(User user);

    void logout();
}
