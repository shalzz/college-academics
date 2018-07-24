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

package com.shalzz.attendance.data.remote.interceptor;

import android.util.Base64;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HeaderInterceptor implements Interceptor {
    private static final String[] composites = new String[]{
            "T42aKlVuy+lWuSyqOSo1obdXgZWK5oZiZzuzOz5Dh1uAJ3jzzV6Tdw==",
            "HdjWQhtavr0n/R2fUxhE1eA5z/nh1bQXMXaAaks28R+zaD2avD/xOA=="
    };

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request newRequest = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent","academics-android-app")
                .header("x-api-key", useXorStringHiding())
                .build();
        return chain.proceed(newRequest);
    }

    private String useXorStringHiding() {
        byte[] xorParts0 = Base64.decode(composites[0],0);
        byte[] xorParts1 = Base64.decode(composites[1], 0);

        byte[] xorKey = new byte[xorParts0.length];
        for(int i = 0; i < xorParts1.length; i++){
            xorKey[i] = (byte) (xorParts0[i] ^ xorParts1[i]);
        }

        return new String(xorKey);
    }
}
