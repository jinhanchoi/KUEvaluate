package kr.ac.korea.oku.emergency.ui.main.evacuee

import android.location.Location
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PathOverlay
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.ui.main.evacuee.adapter.EvacueeDestAdaptor
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc
import kr.ac.korea.oku.emergency.ui.main.locations.PolylineDrawer

interface DirectionDrawable {
    var naverMap: NaverMap
    var polyline: PathOverlay?
    var closestLoc: ClosestLoc?
    var foundPath: MutableList<LatLng>?
    var polylineDrawer : PolylineDrawer?
    val drawAdaptor: EvacueeDestAdaptor
    fun findDirection() : (location : Location, dest : Dest) -> Unit = {
            loc, dest ->
        //to remove line from map
        polyline?.map = null
        foundPath = null
        polylineDrawer?.setPolylineWithDirection(loc, dest)
    }
}