package kr.ac.korea.oku.emergency.ui.main.locations.refactor

import android.location.Location
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils

object MapHandleUtil {
    fun moveMapTo(
        map: NaverMap,
        location : Location,
        bearing : Double? = null,
    ){
        val latlng = LatLng(location)
        val locationOverlay = map.locationOverlay
        val newBearing = bearing?.toFloat() ?: location.bearing
        locationOverlay.position = latlng
        locationOverlay.bearing = newBearing
        locationOverlay.isVisible = true

        val newPos = CameraPosition(
            latlng,
            map.cameraPosition.zoom,
            0.0,
            newBearing.toDouble()
        )
        map.moveCamera(CameraUpdate.toCameraPosition(newPos))
        //map.moveCamera(CameraUpdate.scrollTo(latlng))

    }

    fun changeSelectedMarker(currentMarker: Marker?, map: NaverMap ,item: Dest) : Marker{
        currentMarker?.let { it.map = null }

        val latLng = LatLng(item.lat, item.lon)
        val newMarker = Marker()
        newMarker.position = latLng
        newMarker.icon = OverlayImage.fromResource(R.drawable.ic_location_red_24)
        newMarker.map = map

        val cameraUpdate = CameraUpdate.scrollTo(latLng).animate(
            CameraAnimation.Easing
        )
        map.moveCamera(cameraUpdate)
        return newMarker
    }

    fun findPoint(list: List<LatLng>, divider:Int): LatLng {
        val size = list.size
        val dividePoint = (size/divider)

        return list[dividePoint]
    }
}