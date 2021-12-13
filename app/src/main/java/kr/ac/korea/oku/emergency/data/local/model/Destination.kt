package kr.ac.korea.oku.emergency.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "Destinations")
data class Destination(
    @PrimaryKey(autoGenerate = true)
    val id : Int? = null,

    @ColumnInfo(name = "name")
    val name : String,

    @ColumnInfo(name = "address")
    val address : String,

    @ColumnInfo(name = "lat")
    val lat : Double,

    @ColumnInfo(name = "lon")
    val lon : Double
)

data class Dest(
    val id : Int? = null,

    val isMeter: Boolean = false,

    val name : String,

    val address : String,

    val lat : Double,

    val lon : Double,

    val distance : Double,

    var totalTime : Int = 0
)