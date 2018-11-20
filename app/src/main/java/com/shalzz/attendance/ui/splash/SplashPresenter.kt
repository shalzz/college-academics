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