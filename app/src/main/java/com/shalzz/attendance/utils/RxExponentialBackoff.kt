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
