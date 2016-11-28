package com.shalzz.attendance.ui.attendance;

import com.shalzz.attendance.model.remote.Subject;
import com.shalzz.attendance.network.RetrofitException;
import com.shalzz.attendance.ui.base.MvpView;

import java.util.List;

/**
 * @author shalzz
 */
interface AttendanceMvpView extends MvpView {
    void clearSubjects();

    void addSubjects(List<Subject> subjects);

    void showcaseView();

    void updateLastSync();

    void stopRefreshing();

    void showError(String message);

    void showRetryError(String message);

    void showEmptyView(boolean show);

    void showErrorView(RetrofitException error);
}
