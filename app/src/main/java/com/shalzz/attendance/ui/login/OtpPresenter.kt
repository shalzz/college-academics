package com.shalzz.attendance.ui.login

import android.content.Context
import com.shalzz.attendance.R
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.NetworkUtil
import com.shalzz.attendance.utils.RxExponentialBackoff
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * @author shalzz
 */
@ConfigPersistent
class OtpPresenter @Inject
internal constructor(private val mDataManager: DataManager,
    private val mPreferenceHelper: PreferencesHelper,
    @param:ApplicationContext private val mContext: Context
) : BasePresenter<OtpMvpView>() {

    private var mDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: OtpMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    fun verifyOTP(phone: String, otp: Number) {
        checkViewAttached()
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            Timber.i("Sync canceled, connection not available")
            mvpView.showError(mContext.getString(R.string.no_internet))
            return
        }

        val onError = { error : Throwable ->
            if (error !is RetrofitException) {
                Timber.e(error)
            }
            else if (isViewAttached) {
                if (error.kind == RetrofitException.Kind.HTTP) {
                    mvpView.showError(error.response.message())
                } else {
                    mvpView.showError(error.message)
                    Timber.e(error)
                }
            }
        }

        mvpView.showProgressDialog()
        RxUtil.dispose(mDisposable)
        mDisposable = mDataManager.verifyOTP(phone, otp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { token ->
                    mPreferenceHelper.saveUser(phone, token.token)
                    mDataManager.sendRegID(regId=mPreferenceHelper.regId!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .retryWhen(RxExponentialBackoff.maxCount(3))
                        .subscribe( { result->
                            Timber.d("Sent regId to server successfully: %b", result)
                            mvpView.successfulLogin(token.token)
                        }, onError )
                }, onError)
    }
}