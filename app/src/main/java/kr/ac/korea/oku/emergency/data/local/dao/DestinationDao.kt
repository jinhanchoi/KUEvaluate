package kr.ac.korea.oku.emergency.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.ac.korea.oku.emergency.data.local.model.Destination

@Dao
interface DestinationDao {
    @Query("SELECT * FROM Destinations")
    fun getAll() : Flow<List<Destination>>
}