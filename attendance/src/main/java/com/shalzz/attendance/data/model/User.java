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

package com.shalzz.attendance.data.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.shalzz.attendance.model.UserModel;
import com.squareup.sqldelight.RowMapper;

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
@AutoValue
public abstract class User implements UserModel, Parcelable {
    public static final Factory<User> FACTORY = new Factory<>(AutoValue_User::new);

    public static final RowMapper<User> MAPPER = FACTORY.select_allMapper();

    public static TypeAdapter<User> typeAdapter(Gson gson) {
        return new AutoValue_User.GsonTypeAdapter(gson);
    }
}
