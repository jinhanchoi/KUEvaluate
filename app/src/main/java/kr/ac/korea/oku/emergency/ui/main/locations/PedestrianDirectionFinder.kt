package kr.ac.korea.oku.emergency.ui.main.locations

import android.util.Log
import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.data.remote.TmapApiService
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils
import retrofit2.await

class PedestrianDirectionFinder(private val locationApi : TmapApiService): DirectionsFinder {

    override suspend fun findDirection(from : LatLng, to : LatLng) : Path {
        val call = locationApi.requestPedestrian(
            startX = "${from.longitude}",
            startY = "${from.latitude}",
            endX = "${to.longitude}",
            endY = "${to.latitude}",
            startName = "TestStart",
            endName = "TestEnd"
        )
        val result = call.await()
        val allCoords = result.features.filter { it.geometry.type == "LineString" }
            .map { it.geometry.coordinates }
            .flatten()
            .map { 
                val latlng = it as List<Double>
                LatLng(latlng[1],latlng[0])
            }
        if(allCoords.isEmpty()) {
            return Path()

        }
        return Path(
            distance = result.features[0].properties.totalDistance,
            totalTime = result.features[0].properties.totalTime,
            foundPath = findDetailPath(allCoords)
        )
    }

    override suspend fun findDirectionWithBusStop(from: LatLng, to: LatLng): Path {
        TODO("Not yet implemented")
    }

    private fun findDetailPath(coords : List<LatLng>) : MutableList<LatLng> {
        val pathSet = mutableSetOf<LatLng>()
        coords.withIndex().forEach {
            if (it.index + 1 < coords.size) {
                pathSet.addAll(CoordCalcUtils.getAllCoords(it.value, coords[it.index + 1]))
            }
        }

        Log.i("Calc Result", "${pathSet.size} , ${coords.size}")
        return pathSet.toMutableList()
    }
}