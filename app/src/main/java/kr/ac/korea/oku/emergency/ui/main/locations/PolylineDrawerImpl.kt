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
import kr.ac.korea.oku.emergency.ui.main.evacuee.DirectionDrawable
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.DirectionViewModel

class PolylineDrawerImpl(
    private val fragment : DirectionDrawable,
    private val directionsFinder : DirectionsFinder,
    private val sharedViewModel : DirectionViewModel,
): PolylineDrawer {
    override val caller: DirectionDrawable
    get() = fragment
    override val finder: DirectionsFinder
    get() = directionsFinder


    override fun setPolylineWithDirection(loc: Location, dest: Dest) = CoroutineScope(Dispatchers.Main).launch {
        val poly = getPolylineTask(loc,dest)
        poly?.map = caller.naverMap
    }
    private suspend fun getPolylineTask(loc: Location, dest: Dest) = CoroutineScope(Dispatchers.Default).async {
        val directionResult = finder.findDirection(
            LatLng(loc.latitude,loc.longitude),
            LatLng(dest.lat, dest.lon)
        )
        caller.foundPath = directionResult.foundPath.toMutableList()

        sharedViewModel.directions.postValue(caller.foundPath)
        sharedViewModel.pathResult.postValue(directionResult)

        caller.closestLoc = ClosestLoc(caller.foundPath!![0],0, Double.MAX_VALUE)
        caller.foundPath?.let {
            caller.polyline = drawPolyLine(it)
        }

        return@async caller.polyline
    }.await()

    private fun drawPolyLine(foundPath : List<LatLng>) : PathOverlay {
        val polyline = PathOverlay()
        if(foundPath.size > 1) {
            polyline.patternImage = OverlayImage.fromResource(R.drawable.ic_upload)
            polyline.patternInterval = 100
            polyline.width = 20
            polyline.color = Color.BLUE
            polyline.coords = foundPath
        }
        return polyline
    }
}