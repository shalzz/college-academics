package com.shalzz.attendance.utils

import com.shalzz.attendance.data.remote.RetrofitException
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.BiFunction
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * @author shalzz
 */
object RxExponentialBackoff {

    fun maxCount(maxCount: Int): ((t: Observable<Throwable>) ->  ObservableSource<*>) {
        return { throwableObservable: Observable<Throwable> ->
            throwableObservable.zipWith(
                Observable.range(0, maxCount + 1),
                BiFunction<Throwable, Int, Int>  { error: Throwable, retryCount: Int ->
                    when {
                        error is RetrofitException
                            && error.kind == RetrofitException.Kind.NETWORK -> throw error
                        retryCount > maxCount -> throw error
                        else -> {
                            Timber.d(error, "retrying error: %d time", retryCount)
                            retryCount
                        }
                    }
                }
            )
                .flatMap { i ->
                    val backoffDelay = 500 * Math.pow(2.0, i.toDouble())
                    Timber.d("delay retry by: %d ms", backoffDelay.toLong())
                    Observable.timer(backoffDelay.toLong(), TimeUnit.MICROSECONDS)
                }
        }
    }
}
