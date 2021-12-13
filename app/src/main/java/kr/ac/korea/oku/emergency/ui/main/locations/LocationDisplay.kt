package kr.ac.korea.oku.emergency.ui.main.locations

import android.location.Location
import android.util.Log
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils

class LocationDisplay(
    private val path: Path,
    private val map: NaverMap,
    private val checkWaypointFun: (nextLoc:LatLng) -> Unit
) {
    fun display(gpsLocation: Location) {
        Log.i("#Location Displayer", "$gpsLocation")
        Log.i("#path","${path.foundPath.size}")

        if(path.foundPath.isEmpty()) return
        //특정 거리 안에서 가장 가까운 좌표를 찾아서 현재 위치로 표시
        val (checkStartIdx: Int?, minDistIdx) = findClosestCoord(gpsLocation)
        //display
        if(checkStartIdx == null) {
            Log.i("#Move By Map Handler","$gpsLocation")
            MapHandleUtil.moveMapTo(map,gpsLocation)
        }else{
            val nextLoc = path.foundPath[minDistIdx]
            val newBearing = calcNextBearing(minDistIdx, nextLoc)

            checkWaypointFun(nextLoc)

            moveLocationOverlay(nextLoc,newBearing.toFloat())
            moveMapTo(nextLoc,newBearing)
            calcRemainPath(minDistIdx)
        }
    }
    private fun findClosestCoord(gpsLocation: Location): Pair<Int?, Int> {
        var checkStartIdx: Int? = null
        var minDist: Double = Double.MAX_VALUE
        var minDistIdx = 0

        //find closest Loc
        for (locWithIdx in path.foundPath.withIndex()) {
            val loc = locWithIdx.value
            val dist = CoordCalcUtils.calcDistance(
                gpsLocation.latitude,
                loc.latitude,
                gpsLocation.longitude,
                loc.longitude
            ) * 1000

            if (dist <= distanceLimit) {
                if (checkStartIdx == null) {
                    checkStartIdx = locWithIdx.index
                    if (minDist > dist) {
                        minDistIdx = locWithIdx.index
                        minDist = dist
                    }
                } else {
                    if (minDist > dist) {
                        minDistIdx = locWithIdx.index
                        minDist = dist
                    }
                }
                Log.i("#Found closest Dist", "$dist")
            }
            if (checkStartIdx != null && dist > distanceLimit) {
                Log.i("#Stop find closest", "$dist")
                break
            }
        }
        return Pair(checkStartIdx, minDistIdx)
    }

    private fun calcNextBearing(
        minDistIdx: Int,
        loc: LatLng
    ): Double {
        val nextIdx = if (minDistIdx + 1 >= path.foundPath.size) {
            path.foundPath.size - 1
        } else {
            minDistIdx + 1
        }

        return CoordCalcUtils.calculateBearing(
            LatLng(loc.latitude, loc.longitude),
            LatLng(path.foundPath[nextIdx].latitude, path.foundPath[nextIdx].longitude)
        )
    }

    private fun moveLocationOverlay(location: LatLng, newBearing: Float){
        with(map.locationOverlay){
            position = location
            bearing = newBearing
            isVisible = true
        }
    }

    private fun moveMapTo(location: LatLng, newBearing: Double){
        val newPos = CameraPosition(
            location,
            map.cameraPosition.zoom,
            0.0,
            newBearing
        )
        Log.i("#Move By moveCamera", "$newBearing")
        map.moveCamera(CameraUpdate.toCameraPosition(newPos))
    }
    private fun calcRemainPath(startIdx:Int) {
        path.foundPath = path.foundPath.subList(startIdx,path.foundPath.size)
    }

    companion object {
        const val distanceLimit = 10
        const val degreeLimit = 90
    }
}