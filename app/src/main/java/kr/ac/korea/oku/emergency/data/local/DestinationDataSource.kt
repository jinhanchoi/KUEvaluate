package kr.ac.korea.oku.emergency.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kr.ac.korea.oku.emergency.data.local.dao.DestinationDao
import kr.ac.korea.oku.emergency.data.local.model.Destination

@Database(entities = [Destination::class], version = 1,  exportSchema = false)
abstract class DestinationDataSource : RoomDatabase(){
    abstract fun destinationDao() : DestinationDao
    companion object {
        @Volatile
        private var INSTANCE : DestinationDataSource? = null

        fun getDatasource(context : Context) : DestinationDataSource =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DestinationDataSource::class.java,
                    "dest_datasource"
                ).build().also {
                    INSTANCE = it
                }
            }
    }
}