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
        @Query("waypoints") waypoints: String? = null,
    ) : Call<ResultPath>
}
data class ResultPath(
    val route : Result_trackoption?,
    val message : String,
    val code : Int
)
data class Result_trackoption(
    val traoptimal : List<Result_path> = emptyList()
)
data class Result_path(
    val summary : Result_distance,
    val path : List<List<Double>>,
    val guide : List<Guide>,
)

data class Result_distance(
    val distance : Int,
    val waypoints: List<Waypoint>,
    val duration: Int,
)

data class Waypoint(
    val location: List<Double>,
    val distance: Int,
    val duration: Int,
    val pointIndex: Int,
)

data class Guide(
    val pointIndex: Int,
    val type: Int,
    val instructions: String,
    val distance: Int,
    val duration: Int,
)