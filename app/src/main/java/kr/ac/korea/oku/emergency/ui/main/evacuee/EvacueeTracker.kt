package kr.ac.korea.oku.emergency.ui.main.evacuee

import android.widget.Toast
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.LocationOverlay
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils

class EvacueeTracker(
    private val caller : EvacueeFragment,
    private val naverMap : NaverMap
) : LocationCallback() {
    private var trackFailCount = 0
    override fun onLocationResult(locationResult: LocationResult?) {

        val lastLocation = locationResult?.lastLocation ?: return

        val lastLatLng = LatLng(lastLocation)
        val locationOverlay = naverMap.locationOverlay

        trackNearestLatLngFromFoundedPath(lastLatLng)
        changeCameraViewLikeNavi(locationOverlay)
        checkAndChangeStartBtnStatus(locationOverlay)
    }

    private fun trackNearestLatLngFromFoundedPath(lastLoc : LatLng){
        caller.pedFoundPath?.let{
                path ->
            caller.pedClosestLoc?.let {
                    loc ->
                var nextDistance = CoordCalcUtils.calcDistance(
                    lastLoc.latitude,loc.location.latitude,
                    lastLoc.longitude, loc.location.longitude) * 1000
                var idx = loc.idx

                while(true){
                    if(nextDistance < 20) {
                        trackFailCount = 0
                        break
                    }
                    if(idx + 1 >= path.size) {
                        idx = loc.idx
                        trackFailCount++
                        if(trackFailCount > 10) {
                            Toast.makeText(caller.context, "경로에서 벗어 났습니다.", Toast.LENGTH_SHORT).show()
                            trackFailCount = 0
                        }
                        break
                    }
                    val tempLoc = path[idx]
                    nextDistance = CoordCalcUtils.calcDistance(
                        lastLoc.latitude,tempLoc.latitude,
                        lastLoc.longitude, tempLoc.longitude) * 1000
                    idx += 1
                }

                if( CoordCalcUtils.calcDistance(
                        lastLoc.latitude, loc.location.latitude,
                        lastLoc.longitude, loc.location.longitude) * 1000 >= nextDistance
                ) {
                    caller.pedClosestLoc = ClosestLoc(path[idx],idx, nextDistance)
                }
            }
        }
    }
    private fun changeCameraViewLikeNavi(overlay: LocationOverlay) {
        caller.pedClosestLoc?.let{
                closestLoc ->
            caller.pedFoundPath?.let {
                    path ->
                val next = closestLoc.idx+1
                if(next <= path.size) {
                    overlay.bearing = CoordCalcUtils.calculateBearing(closestLoc.location,path[next]).toFloat()
                    overlay.position = closestLoc.location
//                    naverMap.moveCamera(CameraUpdate.scrollTo(closestLoc.location))
                    naverMap.cameraPosition = CameraPosition(
                        closestLoc.location,
                        naverMap.cameraPosition.zoom,
                        0.0,
                        CoordCalcUtils.calculateBearing(closestLoc.location,path[next])
                    )
                }
            }
        }
    }

    private fun checkAndChangeStartBtnStatus(overlay : LocationOverlay) {
        if (caller.waiting) {
            caller.waiting = false
            caller.fab?.setImageResource(R.drawable.ic_location_disabled_black_24dp)
            overlay.isVisible = true
        }
    }
}