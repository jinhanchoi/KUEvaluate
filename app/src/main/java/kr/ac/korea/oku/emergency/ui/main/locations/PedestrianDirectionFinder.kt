package kr.ac.korea.oku.emergency.ui.main.locations

import android.util.Log
import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.data.remote.TmapApiService
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils
import retrofit2.await

class PedestrianDirectionFinder(private val locationApi : TmapApiService) {
    suspend fun findDirection(from : LatLng, to : LatLng) : MutableList<LatLng>{
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
        return findDetailPath(allCoords)
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