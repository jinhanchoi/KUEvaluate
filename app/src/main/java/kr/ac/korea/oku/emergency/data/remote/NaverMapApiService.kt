package kr.ac.korea.oku.emergency.data.remote

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverMapApiService {
    @GET("/map-direction/v1/driving")
    fun requestPath(
        @Header("X-NCP-APIGW-API-KEY-ID") apiKeyID: String,
        @Header("X-NCP-APIGW-API-KEY") apiKey: String,
        @Query("start") start: String,
        @Query("goal") goal: String,
    ) : Call<ResultPath>
}
data class ResultPath(
    val route : Result_trackoption,
    val message : String,
    val code : Int
)
data class Result_trackoption(
    val traoptimal : List<Result_path>
)
data class Result_path(
    val summary : Result_distance,
    val path : List<List<Double>>
)
data class Result_distance(
    val distance : Int
)