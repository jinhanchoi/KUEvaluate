package kr.ac.korea.oku.emergency.ui.main.locations.refactor

import android.view.View
import co.kr.tamer.aos.trunk.ui.utils.gps.GpsClient
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel

interface LocationTrackable {
    var gpsClient: GpsClient
    val locationTracker : LocationTracker
    val locationViewModel: LocationViewModel

    fun getView(): View?
    fun startTracking() {
        getView()?.let{
            locationTracker.enable()
            locationTracker.currentLocationTracking(it, locationViewModel)
        }
    }
    fun getLocationOnce() {
        getView()?.let {
            locationTracker.disable()
            locationTracker.currentLocationTracking(it, locationViewModel)
        }
    }
    fun openGpsDialog(
        onPositiveClicked: () -> Unit,
        onNegativeClicked: () -> Unit
    )
    fun openGpsSetting()
    fun requestGpsProviderIfNeeded()
}