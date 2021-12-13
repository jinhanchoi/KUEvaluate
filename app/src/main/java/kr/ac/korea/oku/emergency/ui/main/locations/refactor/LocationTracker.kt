package kr.ac.korea.oku.emergency.ui.main.locations.refactor

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import java.util.concurrent.atomic.AtomicBoolean

interface LocationTracker{
    fun enable()
    fun disable()
    fun currentLocationTracking(
        caller : View,
        viewModel : LocationViewModel
    )
    data class TrackingLatch(
        var tracking: AtomicBoolean = AtomicBoolean(false)
    )
}
class LocationTrackerImpl(
    private val latch: LocationTracker.TrackingLatch = LocationTracker.TrackingLatch()
): LocationTracker {
    private fun withPermission(
        view : View,
        logic : () -> Unit
    ) {
        Dexter.withContext(view.context)
            .withPermissions(PERMISSIONS)
            .withListener(PermissionsListener(view, logic))
            .check()
    }

    override fun enable() {
        latch.tracking.compareAndSet(false, true)
    }

    override fun disable() {
        latch.tracking.compareAndSet(true,false)
    }

    override fun currentLocationTracking(
        caller : View,
        viewModel : LocationViewModel
    ) {
        withPermission(caller){
            caller.context?.let {
                GoogleApiClient.Builder(it)
                    .addConnectionCallbacks(
                        LocationConnectionCallbackImpl(
                            caller,
                            viewModel
                        )
                    )
                    .addApi(LocationServices.API)
                    .build()
                    .connect()
            }
        }
    }

    companion object {
        private const val TAG = "LocationTracker"
        private const val LOCATION_REQUEST_INTERVAL = 600
        private const val PERMISSION_REQUEST_CODE = 100
        private val PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    inner class LocationConnectionCallbackImpl(
        private val view: View,
        private val viewModel: LocationViewModel,
    ) : GoogleApiClient.ConnectionCallbacks {
        @SuppressLint("MissingPermission")
        override fun onConnected(bundle: Bundle?) {
            val fused = LocationServices.getFusedLocationProviderClient(view.context)
            fused.lastLocation.addOnSuccessListener { location ->
                val locationRequest = LocationRequest().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = LOCATION_REQUEST_INTERVAL.toLong()
                    fastestInterval = LOCATION_REQUEST_INTERVAL.toLong()
                }
                val callback =  LocationCallbackImpl(fused, viewModel)
                fused.requestLocationUpdates(
                    locationRequest,
                    callback,
                    null
                )
            }
        }
        override fun onConnectionSuspended(i: Int) {}
    }

    inner class LocationCallbackImpl(
        private val fused: FusedLocationProviderClient,
        private val viewModel: LocationViewModel
    ) : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
//            for( location in locationResult.locations){
//                viewModel.currentLocation.postValue(location)
//                if(!latch.tracking.get()){
//                    fused.removeLocationUpdates(this)
//                }
//            }
            val lastLoc = locationResult.lastLocation
            viewModel.currentLocation.postValue(lastLoc)
            if(!latch.tracking.get()) {
                fused.removeLocationUpdates(this)
            }
        }
    }

    inner class PermissionsListener(
        private val view: View,
        private val logic : () -> Unit
    ) : MultiplePermissionsListener {
        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
            if (report != null) {
                if (report.areAllPermissionsGranted()) {
                    logic()
                } else {
                    Log.i("PERMISSION", "DENY")
                }
            } else {
                Log.i("PERMISSION", "ERROR")
            }
        }
        override fun onPermissionRationaleShouldBeShown(
            requests: MutableList<PermissionRequest>?,
            token: PermissionToken?
        ) {
            token?.continuePermissionRequest()
        }

    }
}