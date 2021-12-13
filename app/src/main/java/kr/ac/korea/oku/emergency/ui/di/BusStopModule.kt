package kr.ac.korea.oku.emergency.ui.di

import com.tickaroo.tikxml.TikXml
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.ac.korea.oku.emergency.data.remote.BusStopApiService
import kr.ac.korea.oku.emergency.data.remote.TmapApiService
import kr.ac.korea.oku.emergency.ui.main.locations.BusStopFinder
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object BusStopModule {
    private const val url = "http://ws.bus.go.kr"
    @Provides
    internal fun provideBusStopApiService(okHttpClient: OkHttpClient) : BusStopApiService {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(
                TikXmlConverterFactory.create(
                    TikXml.Builder()
                        .exceptionOnUnreadXml(false)
                        .build()
                )
            )
            .baseUrl(url)
            .client(okHttpClient)
            .build()
        return retrofit.create(BusStopApiService::class.java)
    }

    @Provides
    fun provideBusStopFinder(busStopApiService: BusStopApiService) : BusStopFinder {
        return BusStopFinder(busStopApiService)
    }
}