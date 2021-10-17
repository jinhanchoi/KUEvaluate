package kr.ac.korea.oku.emergency.ui.di

import android.content.Context
import co.kr.tamer.aos.trunk.ui.utils.gps.GpsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.data.remote.TmapApiService
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.PedestrianDirectionFinder

@Module
@InstallIn(ActivityComponent::class)
object GpsModule {
    @Provides
    fun provideGpsClient(
        @ApplicationContext appContext: Context
    ) : GpsClient = GpsClient(appContext)

    @Provides
    fun provideDirectionFinder(
        naverMapApiService: NaverMapApiService
    ) : DirectionFinder = DirectionFinder(naverMapApiService)

    @Provides
    fun providePedestrianDirectionFinder(
        tmapApiService : TmapApiService
    ) : PedestrianDirectionFinder = PedestrianDirectionFinder(tmapApiService)
}