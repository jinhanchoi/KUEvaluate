package kr.ac.korea.oku.emergency.data.remote

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TmapApiService{
    @POST("/tmap/routes/pedestrian")
    fun requestPedestrian(
        @Header("appKey") apiKeyID: String = "l7xx8efd93c0002f4f1bb220b1163c9f3414",
        @Query("startX") startX: String,
        @Query("startY") startY: String,
        @Query("endX") endX: String,
        @Query("endY") endY: String,
        @Query("startName") startName: String,
        @Query("endName") endName: String
    ) : Call<PedestrianResult>
}

data class PedestrianResult(
    val type: String,
    val features : List<Feature>
)

data class Feature(
    val type: String,
    val geometry : Geometry,
    val properties: Property
)

data class Geometry(
    val type : String,
    val coordinates : List<Any>
)

data class Property(
    val totalDistance: Int,
    val totalTime : Int,
    val index : Int,
    val pointIndex : Int,
    val name : String,
    val description: String,
    val direction : String,
    val nearPoiName : String,
    val nearPoiX : String,
    val nearPoiY : String,
    val intersectionName : String,
    val facilityType : String,
    val facilityName : String,
    val turnType : Int,
    val pointType : String
)