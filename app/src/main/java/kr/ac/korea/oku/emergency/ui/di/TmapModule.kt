package kr.ac.korea.oku.emergency.ui.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.ac.korea.oku.emergency.data.remote.TmapApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object TmapModule {

    @Provides
    internal fun provideTmapApiService(okHttpClient: OkHttpClient, tmapBaseUrl: String) : TmapApiService {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://apis.openapi.sk.com")
            .client(okHttpClient)
            .build()
        return retrofit.create(TmapApiService::class.java)
    }
}