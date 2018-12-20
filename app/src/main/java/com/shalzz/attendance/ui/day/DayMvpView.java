/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.ui.day;

import com.shalzz.attendance.data.model.entity.Period;
import com.shalzz.attendance.ui.base.MvpView;

import java.util.List;

/**
 * @author shalzz
 */

public interface DayMvpView extends MvpView {

    void clearDay();

    void setDay(List<Period> day);

    void setRefreshing();

    void stopRefreshing();

    void showError(String message);

    void showRetryError(String message);

    void showNoTimetableEmptyView();

    void showNoConnectionErrorView();

    void showNetworkErrorView(String error);

    void showEmptyView(boolean show);
}
