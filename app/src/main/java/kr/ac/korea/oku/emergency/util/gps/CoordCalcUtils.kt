package kr.ac.korea.oku.emergency.util.gps

import com.naver.maps.geometry.LatLng
import kotlin.math.*

object CoordCalcUtils {
    fun calcDistance(orglat1 : Double, orglat2: Double, orglon1 : Double, orglon2 : Double) : Double{
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

    fun getAllCoords(start : LatLng, end : LatLng) : List<LatLng>{
        val azimuth = calculateBearing(start,end)
        return getLocations(interval = 1, azimuth, start, end)
    }

    fun getLocations(
        interval: Int,
        azimuth: Double,
        start: LatLng,
        end: LatLng
    ): MutableList<LatLng> {
        println(
            "getLocations: " +
                    "\ninterval: " + interval +
                    "\n azimuth: " + azimuth +
                    "\n start: " + start.toString()
        )
        val d = getPathLength(start, end)
        val dist = d.toInt() / interval
        var coveredDist = interval
        val coords: MutableList<LatLng> = mutableListOf()
        coords.add(LatLng(start.latitude, start.longitude))
        var distance = 0
        while (distance < dist) {
            val coord: LatLng = getDestinationLatLng(
                start.latitude, start.longitude, azimuth,
                coveredDist.toDouble()
            )
            coveredDist += interval
            coords.add(coord)
            distance += interval
        }
        coords.add(LatLng(end.latitude, end.longitude))
        return coords
    }

    fun getDestinationLatLng(
        lat: Double,
        lng: Double,
        azimuth: Double,
        distance: Double
    ): LatLng {
        val radiusKm: Double = (6371000 / 1000).toDouble() //Radius of the Earth in km
        val brng = Math.toRadians(azimuth) //Bearing is degrees converted to radians.
        val d = distance / 1000 //Distance m converted to km
        val lat1 = Math.toRadians(lat) //Current dd lat point converted to radians
        val lon1 = Math.toRadians(lng) //Current dd long point converted to radians
        var lat2 = asin(
            sin(lat1) * cos(d / radiusKm) + cos(lat1) * sin(d / radiusKm) * cos(brng)
        )
        var lon2 = lon1 + atan2(
            sin(brng) * sin(d / radiusKm) * cos(lat1),
            cos(d / radiusKm) - sin(lat1) * sin(lat2)
        )
        //convert back to degrees
        lat2 = Math.toDegrees(lat2)
        lon2 = Math.toDegrees(lon2)
        return LatLng(lat2, lon2)
    }

    fun getPathLength(start: LatLng, end: LatLng): Double {
        val lat1Rads = Math.toRadians(start.latitude)
        val lat2Rads = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)

        val a =
            sin(deltaLat / 2) * sin(deltaLat / 2) + cos(lat1Rads) * cos(lat2Rads) * sin(deltaLng / 2) * sin(deltaLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6371000 * c
    }

    /**
     * 방위각 구하기
     * 1.위경도는 지구 중심을 기반으로 함, 라디안 각도로 변환 (시작, 도착)
     * 2.도착지 이동방향 구한다.
     *
     */
    fun calculateBearing(start: LatLng, end: LatLng): Double {
        val startLat = Math.toRadians(start.latitude)
        val startLong = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLong = Math.toRadians(end.longitude)
        var dLong = endLong - startLong
        val dPhi = ln(
            tan(endLat / 2.0 + Math.PI / 4.0) / tan(startLat / 2.0 + Math.PI / 4.0)
        )
        if (Math.abs(dLong) > Math.PI) {
            dLong = if (dLong > 0.0) {
                -(2.0 * Math.PI - dLong)
            } else {
                2.0 * Math.PI + dLong
            }
        }
        return (Math.toDegrees(atan2(dLong, dPhi)) + 360.0) % 360.0
    }
}