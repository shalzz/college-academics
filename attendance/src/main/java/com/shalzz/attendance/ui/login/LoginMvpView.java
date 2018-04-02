package com.shalzz.attendance.ui.login;

import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.ui.base.MvpView;

/**
 * @author shalzz
 */
public interface LoginMvpView extends MvpView {

    void showMainActivity(User user);

    void showError(String message);

    void showProgressDialog();

    void dismissProgressDialog();
}
