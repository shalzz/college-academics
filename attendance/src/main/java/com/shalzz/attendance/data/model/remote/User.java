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

package com.shalzz.attendance.data.model.remote;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.immutables.value.Value;

/** Field names need to be the same
 *  as that of the fields in the
 *  JSON object sent by the REST API,
 *  for {@link Gson} to be able to deserialize it
 *  properly and automatically.
 *
 *  Typical `user` JSON object will be of the format:
 *  {
 *      "sapid": "",
 *      "password": "",
 *      "name": "",
 *      "course": ""
 *  }
 *
 *  which is exposed by the api endpoint /api/v1/me
 *  by the express.js server (upes-api) as of this writing.
 */
@Value.Immutable
@Value.Style(allParameters = true)
public abstract class User implements UserModel {
    public static final Mapper<ImmutableUser> MAPPER =
            new Mapper<>( new Mapper.Creator<ImmutableUser>() {
                @Override
                public ImmutableUser create(String sap_id, String name, String course, String
                        password) {
                    return ImmutableUser.of(sap_id,name,course,password);
                }
            });

    public static final class Marshal extends UserMarshal<Marshal> { }

    @NonNull
    @Override
    @SerializedName("sapid")
    public abstract String sap_id();

    @NonNull
    @Override
    public abstract String name();

    @NonNull
    @Override
    public abstract String course();

    @NonNull
    @Override
    public abstract String password();
}
