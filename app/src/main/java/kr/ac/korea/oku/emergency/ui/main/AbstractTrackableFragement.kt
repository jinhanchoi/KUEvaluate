package kr.ac.korea.oku.emergency.ui.main

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import co.kr.tamer.aos.trunk.ui.utils.gps.GpsClient
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.ui.main.destinations.DestinationFinder
import kr.ac.korea.oku.emergency.ui.main.evacuee.adapter.EvacueeDestAdaptor
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionsFinder
import kr.ac.korea.oku.emergency.ui.main.locations.PedestrianDirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackable
import javax.inject.Inject

@AndroidEntryPoint
abstract class AbstractTrackableFragement : LocationTrackable, Fragment() {
    private var gpsDialog: AlertDialog? = null

    @Inject
    override lateinit var gpsClient: GpsClient

    @Inject
    lateinit var destinationFinder: DestinationFinder

    @Inject
    lateinit var defaultFinder: PedestrianDirectionFinder

    protected var selectedMarker: Marker? = null
    protected val activeMarkers: MutableMap<LatLng, Marker> = mutableMapOf()

    protected val recyclerViewDataAdaptor: EvacueeDestAdaptor by lazy {
        EvacueeDestAdaptor(destClickFn)
    }


    open fun hasDestAdaptor(): Boolean = true
    open fun getDirectionFinder() : DirectionsFinder {
        return defaultFinder
    }

    protected open val destClickFn : (Dest)->Unit = { dest: Dest ->
        Log.i("#DestClickCallback","$dest")
    }

    override fun requestGpsProviderIfNeeded() {
        if (!gpsClient.isGpsEnable) {
            openGpsDialog({
                openGpsSetting()
            }, {

            })
        }
    }

    override fun openGpsDialog(
        onPositiveClicked: () -> Unit,
        onNegativeClicked: () -> Unit
    ) {
        if (gpsDialog?.isShowing != true) {
            gpsDialog = context?.let {
                AlertDialog.Builder(it)
                    .setTitle("GPS 켜세요")
                    .setMessage("GPS 켠다")
                    .setPositiveButton("설정") { dialog, which ->
                        onPositiveClicked.invoke()
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소") { dialog, which ->
                        onNegativeClicked.invoke()
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    override fun openGpsSetting() {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).run {
            addCategory(Intent.CATEGORY_DEFAULT)
            startActivity(this)
        }
    }

    protected fun queryDestWith(
        latitude: Double, longitude: Double, toKm: Int = 3,
    ) {
        activeMarkers.forEach {
            it.value.map = null
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = destinationFinder.findDestinations(
                latitude,
                longitude,
                toKm,
            )
            val timedResult = result.withIndex().map { dest ->
                if(dest.index < 3) {
                    val value = dest.value
                    delay(150)
                    getDirectionFinder().findDirection(
                        LatLng(latitude, longitude),
                        LatLng(value.lat,value.lon)
                    ).totalTime?.let { totalTime ->
                        value.copy(
                            totalTime = totalTime
                        )
                    } ?: dest.value
                } else {
                    dest.value
                }
            }

            if(hasDestAdaptor()) {
                recyclerViewDataAdaptor.updateData(timedResult)
                recyclerViewDataAdaptor.notifyDataSetChanged()
            }
        }
    }

    protected fun registerDataChangeListener(map : NaverMap){
        recyclerViewDataAdaptor.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val targets = recyclerViewDataAdaptor.getItemRange(positionStart, positionStart+itemCount)
                targets.forEach {
                    val latlng =  com.naver.maps.geometry.LatLng(it.lat, it.lon)
                    val marker = Marker()
                    marker.icon = OverlayImage.fromResource(R.drawable.ic_location_24)
                    marker.position = latlng
                    marker.map = map
                    activeMarkers[latlng] = marker
                }
            }
        })
    }
}