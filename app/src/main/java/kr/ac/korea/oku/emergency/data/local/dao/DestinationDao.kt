package kr.ac.korea.oku.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.ac.korea.oku.emergency.data.local.model.Destination

@Dao
interface DestinationDao {
    @Query("SELECT *,  ABS(:lat - lat) + ABS(:lon - lon) as calc" +
            " FROM Destinations" +
            " ORDER BY ABS(:lat - lat) + ABS(:lon - lon) ASC LIMIT 30 OFFSET 20")
    fun getAll(lat : Double, lon : Double) : Flow<List<Destination>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(destList : List<Destination>)
}