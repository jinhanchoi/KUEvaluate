package kr.ac.korea.oku.emergency.ui.main.draw

import android.graphics.Color
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel

interface PathDrawable {
    val pathInfoViewModel: PathInfoViewModel

    fun drawPathPolyline(target: NaverMap): PathOverlay? {
        val pathOverlay = pathInfoViewModel.path.value?.let {
            drawPolyLine(it)
        }
        pathOverlay?.map = target
        return pathOverlay
    }

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