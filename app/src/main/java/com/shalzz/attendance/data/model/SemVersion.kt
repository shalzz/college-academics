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

package com.shalzz.attendance.data.model

/**
 * @author shalzz
 */
data class SemVersion (
    val version: String,
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0
) : Comparable<SemVersion>
{
    init {
        val values = version.removePrefix("v").split(".")
        major = values[0].toInt()
        minor = values[1].toInt()
        patch = values[2].toInt()
    }

    override fun compareTo(other:SemVersion) =
        compareValuesBy(this, other, SemVersion::major, SemVersion::minor, SemVersion::patch)
}