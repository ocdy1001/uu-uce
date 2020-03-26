package com.uu_uce.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "pins")
open class PinData(
    var pinId          : Int,
    var location       : String,
    var difficulty     : Int,
    var type           : String,
    var title          : String,
    var content        : String,
    var status         : Int,
    var predecessorIds : String,
    var followIds      : String
){
    @PrimaryKey(autoGenerate = true) var id : Int = 0
}
