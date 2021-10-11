package kr.ac.korea.oku.emergency.ui.di

import android.content.Context
import co.kr.tamer.aos.trunk.ui.utils.gps.GpsClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ActivityComponent::class)
object GpsModule {
    @Provides
    fun provideGpsClient(
        @ApplicationContext appContext: Context
    ) : GpsClient = GpsClient(appContext)
}