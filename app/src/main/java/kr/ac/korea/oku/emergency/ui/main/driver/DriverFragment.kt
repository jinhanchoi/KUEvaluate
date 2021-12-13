package kr.ac.korea.oku.emergency.ui.main.driver

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.LocationSource
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentDriverBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.destinations.DestinationFinder
import kr.ac.korea.oku.emergency.ui.main.draw.PathDrawable
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Point
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionsFinder
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import javax.inject.Inject

/**
 * 경유지 조회 (버스 정류장 조회 api 사용해서 direction 구하도록 추가 하기
 * 5~10km 안에서 가장 가까운 대피소를 자동으로 목적지로
 *
 */
@AndroidEntryPoint
class DriverFragment : AbstractTrackableFragement(), OnMapReadyCallback, PathDrawable {
    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()
    override val pathInfoViewModel: PathInfoViewModel by activityViewModels()

    lateinit var map: NaverMap
    private var pathOverlay: PathOverlay? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private var _binding: FragmentDriverBinding? = null
    private val binding: FragmentDriverBinding
        get() = _binding!!

    @Inject
    lateinit var directionFinder: DirectionFinder

    override fun getDirectionFinder(): DirectionsFinder {
        return directionFinder
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentDriverBinding.inflate(inflater, container, false).also {
            _binding = it
            _binding?.mapview?.onCreate(savedInstanceState)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("Driver", "View Created !!!!!")

        binding.mapview.getMapAsync(this)
        binding.backBtn.setOnClickListener {

            locationViewModel.currentLocation.postValue(null)
            pathInfoViewModel.path.value = null
            pathInfoViewModel.start.value = null
            pathInfoViewModel.end.value = null
            pathInfoViewModel.summary.value = null

            findNavController().navigate(R.id.action_Driver_to_UserType)
        }
        binding.btnStartNavi.setOnClickListener{
            findNavController().navigate(R.id.action_SelfLocSelect_to_Navi)
        }
    }

    override fun onDestroyView() {
        Log.i("#Driver", "Destroy View")
        super.onDestroyView()
        _binding = null
        locationTracker.disable()
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
        requestGpsProviderIfNeeded()
    }

    override fun onMapReady(naverMap: NaverMap) {
        Log.i("#Driver" , "onMapReady Called")
        this.map = naverMap
        pathInfoViewModel.path.observe(viewLifecycleOwner){
            pathOverlay?.let {
                it.map = null
            }
            pathOverlay = drawPathPolyline(map)
        }

        pathInfoViewModel.start.observe(viewLifecycleOwner){
            setMapStartPoint()
            addStartMarker()
        }
        pathInfoViewModel.end.observe(viewLifecycleOwner){
            addEndMarker()
        }


        locationViewModel.currentLocation.observe(viewLifecycleOwner){ location ->
            if(location == null || pathInfoViewModel.path.value != null) return@observe
            MapHandleUtil.moveMapTo(map,location)
            viewLifecycleOwner.lifecycleScope.launch {
                val near = destinationFinder.getNearRangeInFiveToTenKm(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                near?.let{
                    val start = LatLng(
                        location.latitude,
                        location.longitude
                    )
                    val startPoint = Point(
                        latLng = start, marker = Marker(),
                    ).also{
                        it.marker?.position = start
                        it.marker?.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
                        it.marker?.map = map
                    }
                    val end = LatLng(
                        near.lat,
                        near.lon
                    )

                    val endPoint = Point(
                        latLng = end, marker = Marker(), dest = near,
                    ).also{
                        it.marker?.position = end
                        it.marker?.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
                        it.marker?.map = map
                    }
                    val dest = directionFinder.findDirectionWithBusStop(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ),
                        LatLng(
                            near.lat,
                            near.lon
                        )
                    )
                    if(dest.foundPath.isEmpty()) {
                        Toast.makeText(context,"${dest.resultPath?.message}",Toast.LENGTH_LONG).show()
                        return@let
                    }

                    dest.wayPoints.let {
                        if(it.isNotEmpty()) {
                            it.withIndex().forEach { waypointWithIdx ->
                                val marker = Marker().also { marker ->
                                    marker.position = waypointWithIdx.value
                                    marker.icon = OverlayImage.fromResource(R.drawable.ic_place_green_24dp)
                                    marker.map = map
                                }

                                context?.let { ctx ->
                                    InfoWindow().also{ window ->
                                        window.adapter = object : InfoWindow.DefaultTextAdapter(ctx) {
                                            override fun getText(infoWindow: InfoWindow): CharSequence {
                                                return "경유지${waypointWithIdx.index+1}"
                                            }
                                        }
                                    }.open(marker)
                                }
                            }
                        }
                    }


                    MapHandleUtil.findPoint(dest.foundPath,2)
                    MapHandleUtil.findPoint(dest.foundPath,4)

                    pathInfoViewModel.path.postValue(dest.foundPath.toMutableList())
                    pathInfoViewModel.summary.postValue(dest)
                    pathInfoViewModel.start.postValue(startPoint)
                    pathInfoViewModel.end.postValue(endPoint)
                }
            }
        }
        getLocationOnce()
    }

    private fun addStartMarker() {
        //Add Marker of Start, end
        pathInfoViewModel.start.value?.let { startPoint ->
            startMarker?.map = null
            startPoint.latLng?.let{ latlng ->
                val marker = Marker().also {
                    it.position = latlng
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
                    }.open(marker)
                }
                startMarker = marker
            }
        }
    }

    private fun addEndMarker(){
        pathInfoViewModel.end.value?.let { startPoint ->
            endMarker?.map = null
            startPoint.latLng?.let{ latlng ->
                val marker = Marker().also {
                    it.position = latlng
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
                    }.open(marker)
                }

                endMarker = marker
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