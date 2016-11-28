/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
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

import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.injection.ConfigPersistent;
import com.shalzz.attendance.model.local.Day;
import com.shalzz.attendance.ui.base.BasePresenter;

import java.util.Date;

import javax.inject.Inject;

@ConfigPersistent
class DayPresenter extends BasePresenter<DayMvpView> {

    private DatabaseHandler mDb;

    @Inject
    DayPresenter(DatabaseHandler db) {
        mDb = db;
    }

    @Override
    public void attachView(DayMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
    }

    void loadDay(Date date) {
        checkViewAttached();
        Day day = Day.create(mDb.getAbsentSubjects(date), mDb.getAllPeriods(date));
        if (day.getPeriods().size() == 0) {
            getMvpView().clearDay();
        } else {
            getMvpView().setDay(day);
        }
    }
}
