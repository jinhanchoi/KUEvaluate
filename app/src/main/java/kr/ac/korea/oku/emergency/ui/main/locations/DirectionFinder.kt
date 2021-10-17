package kr.ac.korea.oku.emergency.ui.main.locations

import android.util.Log
import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.data.remote.ResultPath
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils
import retrofit2.await

class DirectionFinder(private val locationApi : NaverMapApiService) {
    suspend fun findDirection(from : LatLng, to : LatLng) : MutableList<LatLng>{
        val call = locationApi.requestPath(
            APIKEY_ID,
            APIKEY,
            "${from.longitude}, ${from.latitude}",
            "${to.longitude}, ${to.latitude}"
        )
        return findDetailPath(call.await())
    }

    private fun findDetailPath(res : ResultPath) : MutableList<LatLng> {
        if (res.route.traoptimal.isEmpty()) {
            return mutableListOf()
        }

        val pathSet = mutableSetOf<LatLng>()
        val pathList = res.route.traoptimal[0].path.map { LatLng(it[1],it[0]) }

        pathList.withIndex().forEach {
            if(it.index + 1 < pathList.size) {
                pathSet.addAll(CoordCalcUtils.getAllCoords(it.value, pathList[it.index + 1]))
            }
        }

        Log.i("Calc Result", "${pathSet.size} , ${pathList.size}")
        return pathSet.toMutableList()
    }

    companion object{
        const val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"
        const val APIKEY_ID = "kio62awlhg"
    }
}