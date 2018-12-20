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
