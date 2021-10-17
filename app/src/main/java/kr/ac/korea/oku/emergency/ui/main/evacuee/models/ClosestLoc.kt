package kr.ac.korea.oku.emergency.ui.main.evacuee.models

import com.naver.maps.geometry.LatLng

data class ClosestLoc(
    val location: LatLng,
    val idx : Int,
    val distance: Double
)