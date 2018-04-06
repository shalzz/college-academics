package com.shalzz.attendance.ui.timetable;

import com.shalzz.attendance.ui.base.MvpView;

import java.util.Date;

/**
 * @author shalzz
 */
public interface TimeTableMvpView extends MvpView {

    void setDate(Date date);

    void scrollToDate(Date date);

}
