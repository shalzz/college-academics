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

package com.shalzz.attendance.data.remote

import com.shalzz.attendance.data.model.College
import com.shalzz.attendance.data.model.TokenModel
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.*

interface DataAPI {

    @GET("me/login")
    fun login(@Header("Authorization") auth: String,
              @Header("x-clg-id") college: String,
              @Query("captcha") captcha: String,
              @Query("cookie") cookie: String): Observable<TokenModel>

    @GET("captcha")
    fun getCaptcha(@Header("x-clg-id") college: String): Observable<retrofit2.Response<ResponseBody>>

    @GET("colleges")
    fun getColleges(): Observable<List<College>>

    @GET("me")
    fun getUser(): Observable<User>

    @get:GET("me/attendance")
    val attendance: Observable<List<Subject>>

    @GET("me/timetable/{date}")
    fun getTimetable(@Path("date") date: String): Observable<List<Period>>

    @FormUrlEncoded
    @POST("me/verify")
    fun verifyValidSignature(
        @Field("data") signedData: String,
        @Field("sig") signature: String
    ): Observable<Boolean>

    // TODO: add logout api
    companion object {
        val API_VERSION = "/api/v5/prod/"
        val ENDPOINT = "https://academics.8bitlabs.tech$API_VERSION"
//         val ENDPOINT = "http://192.168.1.248:3000$API_VERSION"
    }
}
