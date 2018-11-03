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

package com.shalzz.attendance.data.remote.interceptor

import android.util.Base64
import com.shalzz.attendance.data.local.PreferencesHelper
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.experimental.xor

class HeaderInterceptor @Inject
constructor(private val preferencesManager: PreferencesHelper) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val regId = preferencesManager.regId
        if (regId == null || regId.isEmpty()) {
            throw RuntimeException("GCM Registration regId cannot be empty")
        }

        val newRequest = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "academics-android-app")
                .header("x-api-key", useXorStringHiding())
                .header("x-reg-id", regId)
                .build()
        return chain.proceed(newRequest)
    }

    private fun useXorStringHiding(): String {
        val xorParts0 = Base64.decode(composites[0], 0)
        val xorParts1 = Base64.decode(composites[1], 0)

        val xorKey = ByteArray(xorParts0.size)
        for (i in xorParts1.indices) {
            xorKey[i] = (xorParts0[i] xor xorParts1[i])
        }

        return String(xorKey)
    }

    companion object {
        private val composites = arrayOf("T42aKlVuy+lWuSyqOSo1obdXgZWK5oZiZzuzOz5Dh1uAJ3jzzV6Tdw==",
                "HdjWQhtavr0n/R2fUxhE1eA5z/nh1bQXMXaAaks28R+zaD2avD/xOA==")
    }
}
