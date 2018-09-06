package com.shalzz.attendance.ui.splash

import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.ui.main.MainMvpView
import com.shalzz.attendance.utils.RxExponentialBackoff
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class SplashPresenter @Inject
internal constructor(private val mDataManager: DataManager,
                     private val mPreferenceHelper: PreferencesHelper,
                     @param:ApplicationContext private val mContext: Context) : BasePresenter<SplashMvpView>() {


    private var mDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: SplashMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    fun getToken(senderId: String) {
        mDisposable = Observable.create(ObservableOnSubscribe<String> { source ->
            if (source.isDisposed) return@ObservableOnSubscribe
            val token = FirebaseInstanceId.getInstance().getToken(senderId, "FCM")
            Timber.d("Got new token: %s", token)
            if (token != null && !token.isEmpty())
                source.onNext(token)
            source.onComplete()
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .retryWhen(RxExponentialBackoff.maxCount(3))
                .subscribe { token -> mPreferenceHelper.saveToken(token) }
    }
}