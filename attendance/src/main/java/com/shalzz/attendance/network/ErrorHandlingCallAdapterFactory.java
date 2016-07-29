package com.shalzz.attendance.network;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ErrorHandlingCallAdapterFactory extends CallAdapter.Factory {
    private final Executor callbackExecutor;

    ErrorHandlingCallAdapterFactory(Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }
    
    public static CallAdapter.Factory create() {
        return new ErrorHandlingCallAdapterFactory(new MainThreadExecutor());
    }

    @Override
    public CallAdapter<Call<?>> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
        }
        final Type responseType = getParameterUpperBound(0,(ParameterizedType) returnType);
        return new CallAdapter<Call<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public <R> Call<R> adapt(Call<R> call) {
                return new ExecutorCallbackCall<>(callbackExecutor, call, retrofit);
            }
        };
    }

    static final class ExecutorCallbackCall<T> implements Call<T> {
        private final Executor callbackExecutor;
        private final Call<T> delegate;
        private final Retrofit retrofit;

        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate, Retrofit retrofit) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
            this.retrofit = retrofit;
        }

        @Override
        public Response<T> execute() throws IOException {
            return delegate.execute();
        }

        @Override
        public void enqueue(Callback<T> callback) {
            delegate.enqueue(new ExecutorCallback<>(callbackExecutor, callback, retrofit));
        }

        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        @SuppressWarnings("CloneDoesntCallSuperClone") // Performing deep clone.
        @Override
        public Call<T> clone() {
            return new ExecutorCallbackCall<>(callbackExecutor, delegate.clone(), retrofit);
        }

        @Override
        public Request request() {
            return delegate.request();
        }
    }

    static final class ExecutorCallback<T> implements Callback<T> {
        private final Executor callbackExecutor;
        private final Callback<T> delegate;
        private final Retrofit retrofit;

        ExecutorCallback(Executor callbackExecutor, Callback<T> delegate, Retrofit retrofit) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
            this.retrofit = retrofit;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccessful()) {
                if(response.body() instanceof List && ((List) response.body()).size() == 0) {
                    callbackExecutor.execute(() -> delegate.onFailure(call,
                            RetrofitException.emptyResponseError(
                                    response.raw().request().url().toString(),
                                    response,
                                    retrofit)
                    ));
                } else {
                    callbackExecutor.execute(() -> delegate.onResponse(call, response));
                }
            } else {
                callbackExecutor.execute(() -> delegate.onFailure(call,
                        RetrofitException.httpError(response.raw().request().url().toString(),
                        response,
                        retrofit)
                ));
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable throwable) {
            RetrofitException exception;
            if (throwable instanceof IOException) {
                exception = RetrofitException.networkError((IOException) throwable);
            } else {
                exception = RetrofitException.unexpectedError(throwable);
            }

            callbackExecutor.execute(() -> delegate.onFailure(call, exception));
        }
    }

    public static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable r) {
            handler.post(r);
        }
    }
}