package com.example.viajero.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Lugar (
    @PrimaryKey(autoGenerate = true) val id:Int = 0,
    var lugar:String,
    var imagen:String,
    var lat:String,
    var lon:String,
    var orden:String,
    var costoAl:String,
    var costoTrans:String,
    var comentarios:String

)

