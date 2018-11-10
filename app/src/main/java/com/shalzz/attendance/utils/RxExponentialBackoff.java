package com.shalzz.attendance.utils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import timber.log.Timber;

/**
 * @author shalzz
 */
public class RxExponentialBackoff {

    // TODO: fix not exiting
    public static Function<Observable<Throwable>, ObservableSource<?>> maxCount(int maxCount) {
        return throwableObservable ->
                throwableObservable.zipWith(Observable.range(0, maxCount), (n, i) -> i)
                .flatMap(i -> {
                            int backoffDelay = 500 * (int) Math.pow(2, i);
                            Timber.d("delay retry by: %d ms", backoffDelay);
                            return Observable.timer(backoffDelay, TimeUnit.MICROSECONDS);
                        }
                );
    }
}
