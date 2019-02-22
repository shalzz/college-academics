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

package com.shalzz.attendance.data.remote;

import android.content.Context;

import com.shalzz.attendance.R;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RetrofitException extends RuntimeException {
    static RetrofitException httpError(String url, Response response, Retrofit retrofit,
                                              Context context) {
        String message;

        switch (response.code()) {
            case 401:
                message = context.getString(R.string.auth_error);
                break;
            case 400:
            case 403:
                message = context.getString(R.string.forbidden_error);
                break;
            case 404:
                message = context.getString(R.string.generic_server_down);
                break;
            case 407:
                message =  context.getString(R.string.proxy_error);
                break;
            default:
                message =  context.getString(R.string.generic_server_down);
        }

        return new RetrofitException(message, url, response, Kind.HTTP, null, retrofit);
    }

    static RetrofitException emptyResponseError(Retrofit retrofit, Context context) {
        String message = context.getString(R.string.no_data_content);
        return new RetrofitException(message, null, null, Kind.EMPTY_RESPONSE,
                null, retrofit);
    }

    static RetrofitException networkError(IOException exception, Context context) {
        String message = context.getString(R.string.no_connection_title);
        return new RetrofitException(message, null, null, Kind.NETWORK, exception, null);
    }

    static RetrofitException unexpectedError(Throwable exception, Context context) {
        String message = context.getString(R.string.unexpected_error);
        return new RetrofitException(message, null, null, Kind.UNEXPECTED, exception, null);
    }

    /** Identifies the event kind which triggered a {@link RetrofitException}. */
    public enum Kind {
        /** An {@link IOException} occurred while communicating to the server. */
        NETWORK,
        /** A non-200 HTTP status code was received from the server. */
        HTTP,
        /** An empty response body was received from the server. */
        EMPTY_RESPONSE,
        /**
         * An internal error occurred while attempting to execute a request. It is best practice to
         * re-throw this exception so your application crashes.
         */
        UNEXPECTED
    }

    private final String url;
    private final Response response;
    private final Kind kind;
    private final Retrofit retrofit;

    private RetrofitException(String message, String url, Response response, Kind kind, Throwable
            exception, Retrofit retrofit) {
        super(message, exception);
        this.url = url;
        this.response = response;
        this.kind = kind;
        this.retrofit = retrofit;
    }

    /** The request URL which produced the error. */
    public String getUrl() {
        return url;
    }

    /** Response object containing status code, headers, body, etc. */
    public Response getResponse() {
        return response;
    }

    /** The event kind which triggered this error. */
    public Kind getKind() {
        return kind;
    }

    /** The Retrofit this request was executed on */
    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * HTTP response body converted to specified {@code type}. {@code null} if there is no
     * response.
     *
     * @throws IOException if unable to convert the body to the specified {@code type}.
     */
    public <T> T getErrorBodyAs(Class<T> type) throws IOException {
        if (response == null || response.errorBody() == null) {
            return null;
        }
        Converter<ResponseBody, T> converter = retrofit.responseBodyConverter(type, new Annotation[0]);
        return converter.convert(response.errorBody());
    }
}