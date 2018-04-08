package com.shalzz.attendance.ui.day;

import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.ui.base.MvpView;

import java.util.List;

/**
 * @author shalzz
 */

interface DayMvpView extends MvpView {

    void clearDay();

    void setDay(List<Period> day);

    void setRefreshing();

    void stopRefreshing();

    void showError(String message);

    void showNoTimetableEmptyView();

    void showNoConnectionErrorView();

    void showNetworkErrorView(String error);

    void showEmptyView(boolean show);
}
