package com.shalzz.attendance.data.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shalzz.attendance.data.model.entity.User
import io.reactivex.Observable
import io.reactivex.Single

@Dao
interface UserDao {

    @Query("SELECT * FROM user")
    fun getAll(): Observable<User>

    @Query("SELECT * FROM user WHERE id = :id")
    fun getAllById(id: String): Observable<User>

    @Query("SELECT count(*) FROM user")
    fun getCount(): Single<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Query("DELETE from user")
    fun deleteAll()
}