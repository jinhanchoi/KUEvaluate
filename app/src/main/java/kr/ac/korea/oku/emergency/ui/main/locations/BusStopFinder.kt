package kr.ac.korea.oku.emergency.ui.main.locations

import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.data.remote.BusStopApiService
import retrofit2.await

class BusStopFinder(
    private val busStop: BusStopApiService
) {
    suspend fun findNearBusStop(latitude:Double, longitude:Double) : List<Dest> {
        val nearBusStop = busStop.getBusStops(
            tmX = longitude.toString(),
            tmY = latitude.toString()
        ).await()

        return nearBusStop
            .msgBody
            ?.itemList
            ?.map {
                Dest(
                    name = it.stationName ?: "",
                    isMeter = true,
                    address = it.stationName ?: "",
                    lat = it.gpsY?.toDouble() ?: 0.0,
                    lon = it.gpsX?.toDouble() ?: 0.0,
                    distance = it.dist?.toDouble() ?: 0.0,
                )
            } ?: emptyList()
    }
}