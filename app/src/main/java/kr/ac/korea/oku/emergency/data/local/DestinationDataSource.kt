package kr.ac.korea.oku.emergency.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.data.local.dao.DestinationDao
import kr.ac.korea.oku.emergency.data.local.model.Destination
import java.util.concurrent.Executor

@Database(entities = [Destination::class], version = 1,  exportSchema = false)
abstract class DestinationDataSource : RoomDatabase(){
    abstract fun destinationDao() : DestinationDao
    companion object {
        @Volatile
        private var INSTANCE : DestinationDataSource? = null

        fun getDatasource(context : Context, executors : Executor) : DestinationDataSource =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DestinationDataSource::class.java,
                    "dest_datasource"
                ).build().also {
                    INSTANCE = it
                    CoroutineScope(Dispatchers.IO).launch {
                        val database = it
                        val destList = genTargets(20000)
                        database.runInTransaction {
                            database.destinationDao().insertAll(destList)
                        }
                        println("save all completed")
                    }
                }
            }
    }
}

fun genTargets(count : Int) : List<Destination> {
    return listOf(
        Destination(name="벚꽃로", address = "서울 금천구", lat = 37.480637, lon = 126.883177),
        Destination(name="바우뫼로", address = "서울 서초구", lat = 37.472892, lon = 127.030324),
        Destination(name="구로5동", address = "서울 구로구", lat = 37.506344, lon = 126.887169),
        Destination(name="영등포동2가", address = "서울 영등포구", lat = 37.519311, lon = 126.914680)
    )
//    return (1..count).map { Destination(name="Test$it", address = "서울$it", lat = 47.1231, lon = 128.00332) }
}