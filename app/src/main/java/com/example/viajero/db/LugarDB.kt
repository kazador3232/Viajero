package com.example.viajero.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [Lugar::class], version = 1)
abstract class LugarDB : RoomDatabase() {
    abstract fun lugarDao():LugarDao
    companion object {
        @Volatile
        private var BASE_DATOS : LugarDB? = null
        fun getInstance(contexto: Context):LugarDB {
            return BASE_DATOS ?: synchronized(this) {
                Room.databaseBuilder(
                    contexto.applicationContext,
                    LugarDB::class.java,
                    "Viaje.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { BASE_DATOS = it }
            }
        }
    }
}