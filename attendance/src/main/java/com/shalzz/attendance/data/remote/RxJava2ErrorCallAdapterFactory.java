package com.shalzz.attendance.data.remote;

import android.content.Context;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.functions.Func1;

public class RxJava2ErrorCallAdapterFactory extends CallAdapter.Factory {
    private final RxJava2CallAdapterFactory original;
    private final Context mContext;

    private RxJava2ErrorCallAdapterFactory(Context context) {
        mContext = context;
        original = RxJava2CallAdapterFactory.create();
    }

    public static CallAdapter.Factory create(Context context) {
        return new RxJava2ErrorCallAdapterFactory(context);
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return new RxCallAdapterWrapper(retrofit,
                original.get(returnType, annotations, retrofit),
                mContext);
    }

    private static class RxCallAdapterWrapper implements CallAdapter<Observable<?>,Observable<?>> {
        private final Retrofit retrofit;
        private final CallAdapter<?,?> wrapped;
        private final Context context;

        RxCallAdapterWrapper(Retrofit retrofit, CallAdapter<?,?> wrapped, Context context) {
            this.retrofit = retrofit;
            this.wrapped = wrapped;
            this.context = context;
        }

        @Override
        public Type responseType() {
            return wrapped.responseType();
        }

        @Override
        public Observable adapt(Call call) {
            return ((Observable) wrapped.adapt(call)).onErrorResumeNext(new Function<Throwable, ObservableSource>() {
                @Override
                public ObservableSource apply(Throwable throwable) throws Exception {
                    return Observable.error(asRetrofitException(throwable));
                }
            });
        }

        private RetrofitException asRetrofitException(Throwable throwable) {
            // We had non-200 http error
            if (throwable instanceof HttpException) {
                HttpException httpException = (HttpException) throwable;
                Response response = httpException.response();
                return RetrofitException.httpError(response.raw().request().url().toString(),
                        response, retrofit, context);
            }
            // A network error happened
            if (throwable instanceof IOException) {
                return RetrofitException.networkError((IOException) throwable, context);
            }

            // We don't know what happened. We need to simply convert to an unknown error

            return RetrofitException.unexpectedError(throwable, context);
        }
    }
}

