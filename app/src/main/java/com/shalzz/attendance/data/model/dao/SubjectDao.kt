package com.shalzz.attendance.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
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