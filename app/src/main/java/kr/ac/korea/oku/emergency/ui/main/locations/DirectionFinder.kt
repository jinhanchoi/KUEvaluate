package kr.ac.korea.oku.emergency.ui.main.locations

import android.util.Log
import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.data.remote.BusStopApiService
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.data.remote.ResultPath
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils
import retrofit2.await
import java.util.*

class DirectionFinder(
    private val locationApi : NaverMapApiService,
    private val busStop: BusStopApiService
) : DirectionsFinder {

    override suspend fun findDirectionWithBusStop(from: LatLng, to: LatLng): Path {
        val resultPath = locationApi.requestPath(
            APIKEY_ID,
            APIKEY,
            "${from.longitude}, ${from.latitude}",
            "${to.longitude}, ${to.latitude}",
        ).await()

        val path =  resultPath.route?.let{
            it.traoptimal[0].path.map { LatLng(it[1],it[0]) }
        } ?: emptyList()

        if(path.isEmpty()) return Path()

        val halfPoint = findPoint(path,2)
        val quarterPoint = findPoint(path,4)

        val busStopHalf = busStop.getBusStops(
            tmX = halfPoint.longitude.toString(),
            tmY = halfPoint.latitude.toString()
        ).await()

        val busStopQuarter = busStop.getBusStops(
            tmX = quarterPoint.longitude.toString(),
            tmY = quarterPoint.latitude.toString()
        ).await()

//        val busStopHalfX = busStopHalf.msgBody?.itemList?.get(0)?.gpsX
//        val busStopHalfY = busStopHalf.msgBody?.itemList?.get(0)?.gpsY
//
//        val busStopQuarterX = busStopQuarter.msgBody?.itemList?.get(0)?.gpsX
//        val busStopQuarterY = busStopQuarter.msgBody?.itemList?.get(0)?.gpsY
//
//        val waypointsQuery = "$busStopQuarterX,$busStopQuarterY|$busStopHalfX,$busStopHalfY"
        val waypointsQuery = "${quarterPoint.longitude},${quarterPoint.latitude}|${halfPoint.longitude},${halfPoint.latitude}"
        val resultWaypointPath = locationApi.requestPath(
            APIKEY_ID,
            APIKEY,
            "${from.longitude}, ${from.latitude}",
            "${to.longitude}, ${to.latitude}",
            waypoints = waypointsQuery
        ).await()


        return Path(
            foundPath = resultWaypointPath.route?.let{ resultPath ->
                resultPath.traoptimal[0].path.map { LatLng(it[1],it[0]) }
            } ?: emptyList(),
            resultPath = resultWaypointPath,
            wayPoints = resultWaypointPath.route?.let {
                it.traoptimal[0].summary.waypoints.map {
                    LatLng(it.location[1],it.location[0])
                }
            } ?: emptyList()
        )
    }
    override suspend fun findDirection(from : LatLng, to : LatLng) : Path {
//        val result = busStop.getBusStops(
//            tmX = from.longitude.toString(),
//            tmY = from.latitude.toString()
//        ).await()
//        val x = result.msgBody?.itemList?.get(0)?.gpsX
//        val y = result.msgBody?.itemList?.get(0)?.gpsY


        val resultPath = locationApi.requestPath(
            APIKEY_ID,
            APIKEY,
            "${from.longitude}, ${from.latitude}",
            "${to.longitude}, ${to.latitude}",
        ).await()

        return Path(
            totalTime = resultPath.route?.traoptimal?.get(0)?.summary?.duration,
            distance = resultPath.route?.traoptimal?.get(0)?.summary?.distance,
            foundPath = findDetailPath(resultPath),
            resultPath = resultPath
        )
    }

    private fun findDetailPath(res : ResultPath) : MutableList<LatLng> {

        if (Objects.isNull(res.route) || res.route?.traoptimal?.isEmpty() == true) {
            return mutableListOf()
        }
        return res.route?.let {
            it.traoptimal.let { path ->
                val pathSet = mutableSetOf<LatLng>()
                val pathList = path[0].path.map { LatLng(it[1],it[0]) }
                pathList.withIndex().forEach {
                    if(it.index + 1 < pathList.size) {
                        pathSet.addAll(CoordCalcUtils.getAllCoords(it.value, pathList[it.index + 1]))
                    }
                }

                Log.i("Calc Result", "${pathSet.size} , ${pathList.size}")
                return pathSet.toMutableList()
            }
        } ?: mutableListOf()
    }

    private fun findPoint(list: List<LatLng>, divider:Int): LatLng {
        val size = list.size
        val dividePoint = (size/divider)

        return list[dividePoint]
    }
    companion object{
        const val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"
        const val APIKEY_ID = "kio62awlhg"
    }
}