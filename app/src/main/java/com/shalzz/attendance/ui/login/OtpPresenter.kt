package com.shalzz.attendance.ui.login

import android.content.Context
import com.shalzz.attendance.BuildConfig
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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
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

    private var mDisposable: CompositeDisposable = CompositeDisposable()

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: OtpMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    private fun registerAndSyncUser(token: String) {
        mDisposable.add(
            mDataManager.sendRegID(regId=mPreferenceHelper.regId!!)
            .subscribeOn(Schedulers.io())
            .doOnNext {result ->
                Timber.d("Sent regId to server successfully: %b", result)}
            .flatMap { mDataManager.syncUser() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retryWhen(RxExponentialBackoff.maxCount(3))
            .subscribe( {
                mPreferenceHelper.setLoggedIn()
                mvpView.successfulLogin(token)
            }, {error ->
                if (error !is RetrofitException) {
                    mvpView.showError(null)
                    Timber.e(error)
                }
                else if (isViewAttached) {
                    mvpView.showError(error.message)
                    if (error.kind != RetrofitException.Kind.HTTP) {
                        Timber.e(error)
                    }
                }
            } )
        )
    }

    fun verifyOTP(phone: String, otp: Number) {
        checkViewAttached()
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            Timber.i("Connection not available")
            mvpView.showError(mContext.getString(R.string.no_internet))
            return
        }

        val onError = { error : Throwable ->
            if (error !is RetrofitException) {
                Timber.e(error)
            }
            else if (isViewAttached) {
                if (error.kind == RetrofitException.Kind.HTTP && error.response.errorBody() != null) {
                    val msg = JSONObject(error.response.errorBody()!!.string())
                    mvpView.showError(msg.getString("error"))
                } else {
                    mvpView.showError(error.message)
                    Timber.e(error)
                }
            }
        }

        mvpView.showProgressDialog()
        mDisposable.dispose(); mDisposable = CompositeDisposable()
        mDisposable.add(
            mDataManager.verifyOTP(phone, otp, BuildConfig.DEBUG /* Bypass actual otp verification by API*/)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { token ->
                    Timber.d("Got auth token: %s", token.token)
                    mPreferenceHelper.saveUser(phone, token.token)
                    registerAndSyncUser(token.token)
                }, onError)
        )
    }
}