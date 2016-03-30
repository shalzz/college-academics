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

package com.shalzz.attendance.model;

import com.google.gson.Gson;

public class UserModel {

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
    private String sapid;
    private String password;
    private String name;
    private String course;

    public String getSapid() {
        return sapid;
    }

    public void setSapid(String sapid) {
        this.sapid = sapid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "sapid='" + sapid + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", course='" + course + '\'' +
                '}';
    }
}
