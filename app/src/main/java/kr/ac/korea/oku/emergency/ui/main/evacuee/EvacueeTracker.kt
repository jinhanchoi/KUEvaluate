package kr.ac.korea.oku.emergency.ui.main.evacuee

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
    override fun onLocationResult(locationResult: LocationResult?) {

        val lastLocation = locationResult?.lastLocation ?: return

        val lastLatLng = LatLng(lastLocation)
        val locationOverlay = naverMap.locationOverlay

        trackNearestLatLngFromFoundedPath(lastLatLng, locationOverlay)
        changeCameraViewLikeNavi(locationOverlay)
        checkAndchangeStartBtnStatus(locationOverlay)
    }

    private fun trackNearestLatLngFromFoundedPath(lastLoc : LatLng, overlay: LocationOverlay){
        caller.pedFoundPath?.let{
                path ->
            caller.pedClosestLoc?.let {
                    loc ->
                var nextDistance = CoordCalcUtils.calcDistance(
                    lastLoc.latitude,loc.location.latitude,
                    lastLoc.longitude, loc.location.longitude) * 1000
                var idx = loc.idx

                while(true){
                    if(nextDistance < 20) break
                    if(idx + 1 >= path.size) {
                        idx = loc.idx
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
                    naverMap.cameraPosition = CameraPosition(
                        closestLoc.location,
                        16.0,
                        110.0,
                        CoordCalcUtils.calculateBearing(closestLoc.location,path[next])
                    )
                }
            }
        }
    }

    private fun checkAndchangeStartBtnStatus(overlay : LocationOverlay) {
        if (caller.waiting) {
            caller.waiting = false
            caller.fab?.setImageResource(R.drawable.ic_location_disabled_black_24dp)
            overlay.isVisible = true
        }
    }
}