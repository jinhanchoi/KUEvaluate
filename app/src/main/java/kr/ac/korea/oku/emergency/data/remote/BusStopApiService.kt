package kr.ac.korea.oku.emergency.data.remote

import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BusStopApiService {
    @GET("/api/rest/stationinfo/getStationByPos")
    fun getBusStops(
        @Query("tmX") tmX: String = "126.95584930",
        @Query("tmY") tmY: String = "37.53843986",
        @Query("ServiceKey") serviceKey: String = "RsxNAeIynUGQwr0R7jl77ZXfvWjtz5EaJ7dKi/gGCAeNdyiR3+L23lLmY7cLFUb/GM3WzkFH+KAtu2oEz9q2Kw==",
        @Query("radius") radius: Int = 100,
    ) : Call<ServiceResult>
}

@Xml(name = "ServiceResult")
data class ServiceResult(
    @Element
    val msgBody: MsgBody? = null
)

@Xml(name="msgBody")
data class MsgBody(
    @Element(name = "itemList")
    val itemList : List<BusStop>? = emptyList()
)

@Xml
data class BusStop(
    @PropertyElement(name="arsId") var arsId: Int? = null,
    @PropertyElement(name="dist") var dist: Int? = null,
    @PropertyElement(name="gpsX") var gpsX: String? = null,
    @PropertyElement(name="gpsY") var gpsY: String? = null,
    @PropertyElement(name="stationNm") var stationName: String? = null,
)