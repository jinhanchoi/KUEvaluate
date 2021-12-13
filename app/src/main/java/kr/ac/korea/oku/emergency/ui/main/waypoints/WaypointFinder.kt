package kr.ac.korea.oku.emergency.ui.main.waypoints

import kr.ac.korea.oku.emergency.data.local.model.Dest

interface WaypointFinder {
    suspend fun getNearBusStop(latitude: Double, longitude: Double): Dest?
}