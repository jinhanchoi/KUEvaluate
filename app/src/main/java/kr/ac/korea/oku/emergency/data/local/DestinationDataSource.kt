package kr.ac.korea.oku.emergency.data.local

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.opencsv.CSVReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.dao.DestinationDao
import kr.ac.korea.oku.emergency.data.local.model.Destination
import java.io.BufferedReader
import java.io.InputStreamReader
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
                ).addCallback(object : RoomDatabase.Callback(){
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = it
                                val destList = readFromCSV(context.resources)
                                database.runInTransaction {
                                    database.destinationDao().insertAll(destList)
                                }
                                Log.i("SAVE INIT DATA FROM DB", "SUCCESS!!!!")
                            }
                        }
                        Log.i("DB CREATED ONCE", "SUCCESS!!!!")
                    }

                }).build().also {
                    INSTANCE = it
                }
            }

        private fun readFromCSV(resources : Resources) : List<Destination>{
            val resultList = mutableListOf<Destination>()
            val inputStream = InputStreamReader(resources.openRawResource(R.raw.dest_3))
            val buffReader = BufferedReader(inputStream)
            val csvReader = CSVReader(buffReader)
            for( line in csvReader.readAll()){
                resultList.add(
                    Destination(name = line[0], address = line[0], lat = line[2].toDouble(), lon = line[1].toDouble())
                )
            }
            Log.i("Create Data", "SUCCESS!!!!")
            return resultList
        }
    }
}