package com.example.viajero.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LugarDao {
    @Query("SELECT COUNT(*) FROM lugar")
    fun count():Int
    @Query("SELECT * FROM lugar ORDER BY orden ASC")
    fun getAll():List<Lugar>
    @Query("SELECT * FROM lugar WHERE id = :id")
    fun findById(id:Int):Lugar
    @Insert
    fun insert(lugar:Lugar):Long
    @Insert
    fun insertAll(lugares:Lugar)
    @Update
    fun update(lugares:Lugar)
    @Delete
    fun delete(lugar:Lugar)
}