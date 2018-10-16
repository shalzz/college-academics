package com.shalzz.attendance.ui.attendance;

import com.shalzz.attendance.data.model.ListFooter;
import com.shalzz.attendance.data.model.entity.Subject;
import com.shalzz.attendance.ui.base.MvpView;

import java.util.List;

/**
 * @author shalzz
 */
public interface AttendanceMvpView extends MvpView {

    void clearSubjects();

    void addSubjects(List<Subject> subjects);

    void updateFooter(ListFooter footer);

    void showcaseView();

    void setRefreshing();

    void stopRefreshing();

    void showError(String message);

    void showRetryError(String message);

    void showEmptyView(boolean show);

    void showNetworkErrorView(String error);

    void showNoConnectionErrorView();

    void showEmptyErrorView();
}
