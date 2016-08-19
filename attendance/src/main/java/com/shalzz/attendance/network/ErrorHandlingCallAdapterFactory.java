package com.shalzz.attendance.network;

import android.content.Context;
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
    private final Context context;

    private ErrorHandlingCallAdapterFactory(Executor callbackExecutor,Context context) {
        this.callbackExecutor = callbackExecutor;
        this.context = context;
    }
    
    public static CallAdapter.Factory create(Context context) {
        return new ErrorHandlingCallAdapterFactory(new MainThreadExecutor(),context);
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
                return new ExecutorCallbackCall<>(callbackExecutor, call, retrofit, context);
            }
        };
    }

    private static final class ExecutorCallbackCall<T> implements Call<T> {
        private final Executor callbackExecutor;
        private final Call<T> delegate;
        private final Retrofit retrofit;
        private final Context context;


        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate, Retrofit retrofit,
                             Context context) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
            this.retrofit = retrofit;
            this.context = context;
        }

        @Override
        public Response<T> execute() throws IOException {
            return delegate.execute();
        }

        @Override
        public void enqueue(Callback<T> callback) {
            delegate.enqueue(new ExecutorCallback<>(callbackExecutor, callback, retrofit, context));
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
            return new ExecutorCallbackCall<>(callbackExecutor, delegate.clone(), retrofit, context);
        }

        @Override
        public Request request() {
            return delegate.request();
        }
    }

    private static final class ExecutorCallback<T> implements Callback<T> {
        private final Executor callbackExecutor;
        private final Callback<T> delegate;
        private final Retrofit retrofit;
        private final Context context;

        ExecutorCallback(Executor callbackExecutor, Callback<T> delegate, Retrofit retrofit,
                         Context context) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
            this.retrofit = retrofit;
            this.context = context;
        }

        @Override
        public void onResponse(Call<T> call, Response<T> response) {
            if (response.isSuccessful()) {
                if(response.body() instanceof List && ((List) response.body()).size() == 0) {
                    callbackExecutor.execute(() -> delegate.onFailure(call,
                            RetrofitException.emptyResponseError(
                                    response.raw().request().url().toString(),
                                    response,
                                    retrofit,
                                    context)
                    ));
                } else {
                    callbackExecutor.execute(() -> delegate.onResponse(call, response));
                }
            } else {
                callbackExecutor.execute(() -> delegate.onFailure(call,
                        RetrofitException.httpError(response.raw().request().url().toString(),
                                response,
                                retrofit,
                                context)
                ));
            }
        }

        @Override
        public void onFailure(Call<T> call, Throwable throwable) {
            RetrofitException exception;
            if (throwable instanceof IOException) {
                exception = RetrofitException.networkError((IOException) throwable, context);
            } else {
                exception = RetrofitException.unexpectedError(throwable);
            }

            callbackExecutor.execute(() -> delegate.onFailure(call, exception));
        }
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable r) {
            handler.post(r);
        }
    }
}