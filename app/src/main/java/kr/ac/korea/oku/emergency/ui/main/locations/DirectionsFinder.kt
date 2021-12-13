package kr.ac.korea.oku.emergency.ui.main.locations

import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path

interface DirectionsFinder {
    suspend fun findDirection(from : LatLng, to : LatLng) : Path
    suspend fun findDirectionWithBusStop(from : LatLng, to : LatLng): Path
}