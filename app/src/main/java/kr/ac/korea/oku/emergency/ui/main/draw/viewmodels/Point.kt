package kr.ac.korea.oku.emergency.ui.main.draw.viewmodels

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.overlay.Marker
import kr.ac.korea.oku.emergency.data.local.model.Dest

data class Point(
    var marker: Marker? = null,
    var latLng: LatLng? = null,
    var dest: Dest? = null
)