package kr.ac.korea.oku.emergency.ui.main.locations

import android.graphics.Color
import android.location.Location
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.ui.main.evacuee.EvacueeFragment
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc

class PedPolylineDrawer(private val caller : EvacueeFragment, private val finder : PedestrianDirectionFinder) {

    fun setPolylineWithDirection(loc: Location, dest: Dest) = CoroutineScope(Dispatchers.Main).launch {
        val poly = getPolylineTask(loc,dest)
        poly?.map = caller.naverMap
    }
    private suspend fun getPolylineTask(loc: Location, dest: Dest) = CoroutineScope(Dispatchers.Default).async {
        caller.pedFoundPath = finder.findDirection(
            LatLng(loc.latitude,loc.longitude),
            LatLng(dest.lat, dest.lon)
        )
        caller.pedClosestLoc = ClosestLoc(caller.pedFoundPath!![0],0, Double.MAX_VALUE)
        caller.pedFoundPath?.let {
            caller.pedPolyline = drawPolyLine(it)
        }

        return@async caller.pedPolyline
    }.await()

    private fun drawPolyLine(foundPath : List<LatLng>) : PathOverlay {
        val polyline = PathOverlay()
        polyline.patternImage = OverlayImage.fromResource(R.drawable.ic_upload)
        polyline.patternInterval = 100
        polyline.width = 20
        polyline.color = Color.RED
        polyline.coords = foundPath
        return polyline
    }
}