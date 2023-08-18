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

package com.shalzz.attendance.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.ListFooter
import io.reactivex.Observable
import io.reactivex.Single

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subject WHERE name LIKE :name")
    fun getAllLikeName(name: String): Observable<List<Subject>>

    @Query("SELECT sum(attended) as attended, sum(held) as held FROM subject")
    fun getTotalAttendance(): Observable<ListFooter>

    @Query("SELECT count(*) FROM subject")
    fun getCount(): Single<Int>

    @Insert(onConflict = REPLACE)
    fun insert(subject: Subject)

    @Query("DELETE from subject")
    fun deleteAll()
}