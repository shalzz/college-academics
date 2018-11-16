package com.shalzz.attendance.ui.splash

import com.google.firebase.iid.FirebaseInstanceId
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.RxExponentialBackoff
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
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