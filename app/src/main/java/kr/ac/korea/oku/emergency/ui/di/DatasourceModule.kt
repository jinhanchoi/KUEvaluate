package kr.ac.korea.oku.emergency.ui.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.ac.korea.oku.emergency.data.local.DestinationDataSource
import kr.ac.korea.oku.emergency.data.local.dao.DestinationDao

@Module
@InstallIn(SingletonComponent::class)
object DatasourceModule {
    @Provides
    fun provideDatasource(
        @ApplicationContext context: Context
    ) : DestinationDataSource = DestinationDataSource.getDatasource(context)

    fun provideDestinationDao(
        dataSource: DestinationDataSource
    ) : DestinationDao = dataSource.destinationDao()
}