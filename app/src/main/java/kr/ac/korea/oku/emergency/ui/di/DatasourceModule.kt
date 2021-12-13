package kr.ac.korea.oku.emergency.ui.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kr.ac.korea.oku.emergency.data.local.DestinationDataSource
import kr.ac.korea.oku.emergency.data.local.dao.DestinationDao
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.ui.main.destinations.DestinationFinder
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.DistanceCalculator
import java.util.concurrent.Executors

@Module
@InstallIn(SingletonComponent::class)
object DatasourceModule {
    @Provides
    fun provideDatasource(
        @ApplicationContext context: Context
    ) : DestinationDataSource = DestinationDataSource.getDatasource(context, Executors.newSingleThreadExecutor())

    @Provides
    fun provideDestinationDao(
        dataSource: DestinationDataSource
    ) : DestinationDao = dataSource.destinationDao()

    @Provides
    fun destinationFinder(
        destinationDao: DestinationDao
    ): DestinationFinder = object : DestinationFinder {
        override suspend fun findDestinations(latitude: Double, longitude: Double,to: Int): List<Dest> {
            return CoroutineScope(Dispatchers.IO).async {
                return@async destinationDao.getAllNew(latitude,longitude)
                    .map { e ->
                        Dest(
                            id = e.id,
                            name = e.name,
                            address = e.address,
                            lat = e.lat,
                            lon = e.lon,
                            distance = (DistanceCalculator.calcDistance(latitude, e.lat, longitude, e.lon))
                        )
                    }.filter { dest ->
                        dest.distance < to
                    }.sortedBy { e2 ->
                        e2.distance
                    }
            }.await()
        }

        override suspend fun getNearRangeInFiveToTenKm(
            from: Double,
            to: Double,
            latitude: Double,
            longitude: Double
        ): Dest? {
            return CoroutineScope(Dispatchers.IO).async {
                return@async destinationDao.getRangeInFiveToTenKm(
                    from,
                    to,
                    latitude,
                    longitude
                ).map { e ->
                    Dest(
                        id = e.id,
                        name = e.name,
                        address = e.address,
                        lat = e.lat,
                        lon = e.lon,
                        distance = (DistanceCalculator.calcDistance(latitude, e.lat, longitude, e.lon))
                    )
                }.filter { dest ->
                    dest.distance > from * 100 && dest.distance < to * 100
                }.minByOrNull { e2 ->
                    e2.distance
                }
            }.await()
        }
    }
}