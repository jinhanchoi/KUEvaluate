package co.kr.tamer.aos.trunk.ui.utils.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import javax.inject.Inject

class GpsClient @Inject constructor(
    private val appContext: Context
) {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    val isGpsEnable: Boolean
        get() = (appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isProviderEnabled(
            LocationManager.GPS_PROVIDER
        ) ?: false

    fun start(
        onLocationChanged: OnLocationChanged,
        onPermissionDenied: OnPermissionDenied,
        onPermissionError: OnPermissionError,
    ) {
        Dexter.withContext(appContext)
            .withPermissions(PERMISSIONS)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionRationaleShouldBeShown(
                    requests: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report != null) {
                        if (report.areAllPermissionsGranted()) {
                            startGps(onLocationChanged, onPermissionDenied)
                        } else {
                            onPermissionDenied.invoke()
                        }
                    } else {
                        onPermissionError.invoke()
                    }
                }
            })
            .check()
    }

    private fun startGps(
        onLocationChanged: OnLocationChanged,
        onPermissionDenied: OnPermissionDenied
    ) {
        LocationRequest.create().apply {
            interval = 5000L
            fastestInterval = 5000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 1000L
        }.also {
            locationRequest = it
            startFusedLocationProvider(it, onLocationChanged, onPermissionDenied)
        }
    }

    private fun startFusedLocationProvider(
        request: LocationRequest,
        onLocationChanged: OnLocationChanged,
        onPermissionDenied: OnPermissionDenied
    ) {
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionDenied.invoke()
        } else {
            LocationServices.getFusedLocationProviderClient(appContext).apply {
                requestLocationUpdates(
                    request,
                    createLocationCallback(onLocationChanged),
                    Looper.getMainLooper()
                )
            }.also {
                fusedLocationProviderClient = it
            }
        }
    }

    private fun createLocationCallback(
        onLocationChanged: OnLocationChanged
    ): LocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            onLocationChanged(result.lastLocation)
        }
    }

    companion object {
        private const val TAG = "GpsClient"

        private val PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}