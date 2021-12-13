package kr.ac.korea.oku.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.ac.korea.oku.emergency.data.local.model.Destination

@Dao
interface DestinationDao {
    @Query("SELECT DISTINCT *,  ABS(:lat - lat) + ABS(:lon - lon) as calc" +
            " FROM Destinations" +
            " ORDER BY ABS(:lat - lat) + ABS(:lon - lon) ASC LIMIT 200")
    fun getAll(lat : Double, lon : Double) : Flow<List<Destination>>

    @Query("SELECT DISTINCT *,  ABS(:lat - lat) + ABS(:lon - lon) as calc" +
            " FROM Destinations" +
            " ORDER BY ABS(:lat - lat) + ABS(:lon - lon) ASC LIMIT 200")
    fun getAllNew(lat : Double, lon : Double) : List<Destination>

    @Query("SELECT DISTINCT *,  ABS(:lat - lat) + ABS(:lon - lon) as calc" +
            " FROM Destinations" +
            " WHERE calc BETWEEN :from AND :to "+
            " ORDER BY calc ASC LIMIT 50")
    fun getRangeInFiveToTenKm(from: Double = 0.06, to: Double = 0.1, lat : Double, lon : Double) : List<Destination>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(destList : List<Destination>)
}