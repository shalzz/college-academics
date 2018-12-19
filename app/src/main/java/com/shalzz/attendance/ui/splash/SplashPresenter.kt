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

package com.shalzz.attendance.ui.splash

import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.ui.base.BasePresenter
import io.reactivex.disposables.Disposable
import javax.inject.Inject

class SplashPresenter @Inject
internal constructor(private val mPreferenceHelper: PreferencesHelper) : BasePresenter<SplashMvpView>() {

    private var mDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: SplashMvpView) {
        super.attachView(mvpView)
    }

    @Suppress("RedundantOverride")
    override fun detachView() {
        super.detachView()
        // Do not dispose off getRegId disposable here!!
    }
}