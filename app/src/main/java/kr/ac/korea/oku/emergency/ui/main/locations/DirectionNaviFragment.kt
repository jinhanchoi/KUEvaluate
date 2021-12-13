package kr.ac.korea.oku.emergency.ui.main.locations

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentDirectionNaviBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.draw.PathDrawable
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils

@AndroidEntryPoint
class DirectionNaviFragment
    : AbstractTrackableFragement(), OnMapReadyCallback ,PathDrawable{

    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()
    override val pathInfoViewModel: PathInfoViewModel by activityViewModels()

    lateinit var map: NaverMap
    lateinit var locationDisplay: LocationDisplay

    private var _binding : FragmentDirectionNaviBinding? = null
    private val binding: FragmentDirectionNaviBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentDirectionNaviBinding.inflate(inflater, container, false).also {
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
            pathInfoViewModel.path.value = null
            pathInfoViewModel.start.value = null
            pathInfoViewModel.end.value = null
            pathInfoViewModel.summary.value = null

            findNavController().navigate(R.id.action_Nav_to_UserType)
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        map = naverMap
        startTracking()
        setMapStartPoint()
        addStartEndMarker()
        addWaypointsMarker()
        drawPathPolyline(map)

        pathInfoViewModel.summary.value?.let { summary ->
            locationDisplay = LocationDisplay(
                path = summary,
                map = map
            ) { nextLoc ->

                val distToOne = CoordCalcUtils.calcDistance(
                    nextLoc.latitude,
                    summary.wayPoints[0].latitude,
                    nextLoc.longitude,
                    summary.wayPoints[0].longitude
                ) * 1000

                if(distToOne < 3) {
                    binding.tvWaypoint.text = "경유지 2"
                }

                val distToTwo = CoordCalcUtils.calcDistance(
                    nextLoc.latitude,
                    summary.wayPoints[1].latitude,
                    nextLoc.longitude,
                    summary.wayPoints[1].longitude
                ) * 1000

                if(distToTwo < 3) {
                    binding.tvWaypoint.text = "경유지 없음"
                }

                Log.i("#Check contains waypoints", "$distToOne, $distToTwo")
            }
        }

        locationViewModel.currentLocation.observe(viewLifecycleOwner){ location ->
            if(location != null) {
                Log.i(this.javaClass.canonicalName, "Location Tracking - $location")
                locationDisplay.display(location)
//                MapHandleUtil.moveMapTo(map, location)
            }
        }
        addDestinationSummary()
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
        getLocationOnce()
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    @SuppressLint("SetTextI18n")
    private fun addDestinationSummary() {
        view?.let { view ->
            val addressView = binding.tvAddress
            addressView.text = pathInfoViewModel.end.value?.dest?.address ?: ""

            val waypointView = binding.tvWaypoint
            val waypoint1View = binding.tvWaypoint1
            val waypoint2View = binding.tvWaypoint2

            pathInfoViewModel.summary.value?.resultPath?.route?.traoptimal?.get(0).let {
                waypointView.text = it?.summary?.waypoints?.get(0)?.let { "경유지 1"} ?: "경유지 없음"
                waypoint1View.text = it?.summary?.waypoints?.get(0)?.let { "경유지 1"} ?: "경유지 없음"
                waypoint2View.text = it?.summary?.waypoints?.get(1)?.let { "경유지 2"} ?: "경유지 없음"
            }

            pathInfoViewModel.summary.value
                ?.resultPath?.route
                ?.traoptimal?.get(0)
                ?.summary?.let{
                    val durationTv = binding.tvDuration
                    val distanceTv = binding.tvDistance

                    durationTv.text = "${it.duration / 1000 / 60}Min"
                    distanceTv.text = "${it.distance / 1000}Km"
                }
        }
    }
    private fun addWaypointsMarker() {
        pathInfoViewModel.summary.value?.wayPoints?.let { waypoints ->
            waypoints.withIndex().forEach { waypointWithIdx ->
                val marker = Marker().also {
                    it.position = waypointWithIdx.value
                    it.icon = OverlayImage.fromResource(R.drawable.ic_place_green_24dp)
                    it.map = map
                }
                context?.let { ctx ->
                    InfoWindow().also{
                        it.adapter = object : InfoWindow.DefaultTextAdapter(ctx) {
                            override fun getText(infoWindow: InfoWindow): CharSequence {
                                return "경유지${waypointWithIdx.index+1}"
                            }
                        }
                    }.open(marker)
                }
            }
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