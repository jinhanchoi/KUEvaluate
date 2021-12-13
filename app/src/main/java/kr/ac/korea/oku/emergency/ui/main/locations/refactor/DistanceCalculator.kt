package kr.ac.korea.oku.emergency.ui.main.locations.refactor

object DistanceCalculator {
    fun calcDistance(
        orglat1 : Double,
        orglat2: Double,
        orglon1 : Double,
        orglon2 : Double
    ) : Double{
        val lon1 = Math.toRadians(orglon1)
        val lon2 = Math.toRadians(orglon2)
        val lat1 = Math.toRadians(orglat1)
        val lat2 = Math.toRadians(orglat2)

        val dlon: Double = lon2 - lon1
        val dlat: Double = lat2 - lat1
        val a = (Math.pow(Math.sin(dlat / 2), 2.0)
                + (Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2.0)))

        val c = 2 * Math.asin(Math.sqrt(a))
        val r = 6371.0
        return c * r
    }
}