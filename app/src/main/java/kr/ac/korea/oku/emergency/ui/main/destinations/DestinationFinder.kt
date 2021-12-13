package kr.ac.korea.oku.emergency.ui.main.destinations

import kr.ac.korea.oku.emergency.data.local.model.Dest

interface DestinationFinder {
    suspend fun findDestinations(
        latitude: Double,
        longitude: Double,
        to: Int
    ): List<Dest>
    suspend fun getNearRangeInFiveToTenKm(
        from: Double = 0.06,
        to: Double = 0.1,
        latitude: Double,
        longitude: Double
    ): Dest?
}