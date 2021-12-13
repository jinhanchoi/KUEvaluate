package kr.ac.korea.oku.emergency.ui.main.locations

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentPedDirectionNaviBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.draw.PathDrawable
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel

@AndroidEntryPoint
class PedDirectionNaviFragment : AbstractTrackableFragement(), OnMapReadyCallback ,PathDrawable {

    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()
    override val pathInfoViewModel: PathInfoViewModel by activityViewModels()

    lateinit var map: NaverMap
    lateinit var locationDisplay: LocationDisplay

    private var _binding : FragmentPedDirectionNaviBinding? = null
    private val binding: FragmentPedDirectionNaviBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPedDirectionNaviBinding.inflate(inflater, container, false).also {
            _binding = it
            _binding?.mapview?.onCreate(savedInstanceState)
        }.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        binding.btnEndNavi.setOnClickListener {

            locationViewModel.currentLocation.value = null
            locationViewModel.near.value = null
            locationViewModel.nearPed.value = null
            pathInfoViewModel.path.value = null
            pathInfoViewModel.start.value = null
            pathInfoViewModel.end.value = null
            pathInfoViewModel.summary.value = null

            findNavController().navigate(R.id.action_Evacuee_to_SelfLocSelect)
        }
        addDestinationSummary()
    }

    override fun onMapReady(naverMap: NaverMap) {
        map = naverMap
        startTracking()
        setMapStartPoint()
        addStartEndMarker()
        drawPathPolyline(map)

        pathInfoViewModel.summary.value?.let { summary ->
            locationDisplay = LocationDisplay(
                path = summary,
                map = map
            ) { }
        }

        locationViewModel.currentLocation.observe(viewLifecycleOwner){ location ->
            if(location != null) {
                Log.i(this.javaClass.canonicalName,"Location Tracking - $location")
                locationDisplay.display(location)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        locationViewModel.currentLocation.value = null
        locationViewModel.near.value = null
        locationViewModel.nearPed.value = null
        locationViewModel.nearBus.value = null
        locationViewModel.nearBusPed.value = null
        pathInfoViewModel.path.value = null
        pathInfoViewModel.start.value = null
        pathInfoViewModel.end.value = null
        pathInfoViewModel.summary.value = null
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
//        requestGpsProviderIfNeeded()
    }

    @SuppressLint("SetTextI18n")
    private fun addDestinationSummary() {
        view?.let {
            val addressView : TextView = it.findViewById(R.id.tvAddress)
            addressView.text = pathInfoViewModel.end.value?.dest?.address ?: ""

            val durationView : TextView = it.findViewById(R.id.tvDuration)
            val durationMin = pathInfoViewModel.summary.value?.totalTime?.toLong()?.let{ sec ->
                "${sec/60} Min"
            } ?: ""
            durationView.text = durationMin

            val distanceView : TextView = it.findViewById(R.id.tvDistance)
            distanceView.text = "${pathInfoViewModel.summary.value?.distance?.toDouble()?.div(1000)}Km"
        }
    }
    private fun addStartEndMarker() {
        //Add Marker of Start, end
        pathInfoViewModel.start.value?.latLng?.let { startPosition ->
            val startMarker = Marker().also {
                it.position = startPosition
                it.icon = OverlayImage.fromResource(R.drawable.ic_place_blue_24dp)
                it.map = map
            }
            context?.let { ctx ->
                InfoWindow().also{
                    it.adapter = object : InfoWindow.DefaultTextAdapter(ctx) {
                        override fun getText(infoWindow: InfoWindow): CharSequence {
                            return "출발지"
                        }
                    }
                }.open(startMarker)
            }
        }
        pathInfoViewModel.end.value?.latLng?.let { endPosition ->
            val endMarker = Marker().also {
                it.position = endPosition
                it.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
                it.map = map
            }

            context?.let { ctx ->
                InfoWindow().also{
                    it.adapter = object : InfoWindow.DefaultTextAdapter(ctx) {
                        override fun getText(infoWindow: InfoWindow): CharSequence {
                            return "도착지"
                        }
                    }
                }.open(endMarker)
            }
        }
    }
    private fun setMapStartPoint() {
        val cameraTarget = map.cameraPosition.target
        val location = Location("dummy").also {
            it.latitude = pathInfoViewModel.start.value?.latLng?.latitude ?: cameraTarget.latitude
            it.longitude = pathInfoViewModel.start.value?.latLng?.longitude ?: cameraTarget.longitude
        }

        MapHandleUtil.moveMapTo(map,location)
    }
}