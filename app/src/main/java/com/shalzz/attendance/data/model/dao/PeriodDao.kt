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