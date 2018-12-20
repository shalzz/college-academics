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
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shalzz.attendance.data.model.entity.Period
import io.reactivex.Observable
import io.reactivex.Single

@Dao
interface PeriodDao {

    @Query("SELECT * FROM period WHERE date = :date ORDER BY start")
    fun getAllByDate(date: String): Observable<List<Period>>

    @Query("SELECT count(*) FROM period WHERE date = :date")
    fun getCountByDate(date: String): Single<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(periods: List<Period>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(periods: Period)

    @Query("DELETE FROM period WHERE date = :date")
    fun deleteByDate(date: String)

    @Query("DELETE from period")
    fun deleteAll()
}