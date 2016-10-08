package com.shalzz.attendance.ui.day;

import com.shalzz.attendance.model.local.Day;
import com.shalzz.attendance.ui.base.MvpView;

/**
 * @author shalzz
 */

public interface DayMvpView extends MvpView {

    public void clearDay();

    public void setDay(Day day);
}
