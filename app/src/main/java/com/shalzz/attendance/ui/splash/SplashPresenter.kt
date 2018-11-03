package com.shalzz.attendance.ui.splash

import com.google.firebase.iid.FirebaseInstanceId
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.RxExponentialBackoff
import io.reactivex.Observable
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

    fun getToken(senderId: String) {
        mDisposable = Observable.create(ObservableOnSubscribe<String> { source ->
            if (source.isDisposed) return@ObservableOnSubscribe
            val token = FirebaseInstanceId.getInstance().getToken(senderId, "FCM")
            Timber.d("Got new regId: %s", token)
            if (token != null && !token.isEmpty())
                source.onNext(token)
            source.onComplete()
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retryWhen(RxExponentialBackoff.maxCount(3))
                .subscribe { token -> mPreferenceHelper.saveToken(token) }
    }
}