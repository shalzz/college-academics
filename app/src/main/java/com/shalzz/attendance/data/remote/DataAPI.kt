/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
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

package com.shalzz.attendance.data.remote

import com.shalzz.attendance.data.model.SenderModel
import com.shalzz.attendance.data.model.TokenModel
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DataAPI {

    @GET("login/{phone}")
    fun login(@Path("phone") phone: String): Observable<SenderModel>

    @GET("verify-otp/{phone}")
    fun verifyOTP(@Path("phone") phone: String, @Query("otp") otp: Number,
                            @Query("bypass") bypass: Boolean):
            Observable<TokenModel>

    @GET("me")
    fun getUser(): Observable<User>

    @FormUrlEncoded
    @POST("me/regid")
    fun sendRegID(@Field("regid") registerationID: String,
                    @Header("Authorization") auth: String): Observable<Boolean>

    @get:GET("me/attendance")
    val attendance: Observable<List<Subject>>

    @GET("me/timetable/{date}")
    fun getTimetable(@Path("date") date: String): Observable<List<Period>>

    @FormUrlEncoded
    @POST("me/verify")
    fun verifyValidSignature(@Field("data") signedData: String,
                             @Field("sig") signature: String): Observable<Boolean>

    companion object {
//        val API_VERSION = "/v3/prod/"
//        val ENDPOINT = "https://academics.8bitlabs.tech$API_VERSION"
        val API_VERSION = "/"
        val ENDPOINT = "http://192.168.1.160:3000$API_VERSION"
    }

}
