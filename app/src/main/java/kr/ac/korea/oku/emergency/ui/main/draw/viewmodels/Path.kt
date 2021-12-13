package kr.ac.korea.oku.emergency.ui.main.draw.viewmodels

import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.data.remote.ResultPath

data class Path(
    val totalTime : Int? = null,
    val distance : Int? = null,
    var foundPath : List<LatLng> = emptyList(),
    val resultPath : ResultPath? = null,
    val wayPoints : List<LatLng> = emptyList()
)