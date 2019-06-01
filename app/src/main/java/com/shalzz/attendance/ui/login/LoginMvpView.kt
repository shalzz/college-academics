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

package com.shalzz.attendance.ui.login

import com.shalzz.attendance.data.model.College
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.ui.base.MvpView

/**
 * @author shalzz
 */
interface LoginMvpView : MvpView {

    fun saveToken(username: String, college: String, authToken: String)

    fun showMainActivity(user: User, password: String)

    fun showError(message: String?)

    fun updateCollegeList(data: List<College>)

    fun showProgressDialog(msg: String = "Logging in...")

    fun showCaptchaDialog()

    fun dismissProgressDialog()
}