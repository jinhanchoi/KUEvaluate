package kr.ac.korea.oku.emergency.ui.main.evacuee.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.LocationSource
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.CircleOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.databinding.FragmentWithcarevacueeBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractMapTrackingFragment
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.draw.PathDrawable
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Point
import kr.ac.korea.oku.emergency.ui.main.evacuee.adapter.EvacueeDestAdaptor
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc
import kr.ac.korea.oku.emergency.ui.main.locations.*
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import kr.ac.korea.oku.emergency.util.LiveDataExtension.observeOnce
import kr.ac.korea.oku.emergency.util.px
import kr.ac.korea.oku.emergency.util.setHorizontalSpace
import javax.inject.Inject


@AndroidEntryPoint
class WithCarEvacueeFragment : AbstractTrackableFragement(), OnMapReadyCallback, PathDrawable {

    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()
    override val pathInfoViewModel: PathInfoViewModel by activityViewModels()

    lateinit var map: NaverMap
    private var pathOverlay: PathOverlay? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var circleOverlay: CircleOverlay? = null

    private var _binding : FragmentWithcarevacueeBinding? = null
    private val binding: FragmentWithcarevacueeBinding
        get() = _binding!!

    @Inject
    lateinit var busStopFinder: BusStopFinder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentWithcarevacueeBinding.inflate(inflater, container, false).also {
            _binding = it
            _binding?.mapview?.onCreate(savedInstanceState)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        binding.backBtn.setOnClickListener {
            locationViewModel.currentLocation.value = null
            locationViewModel.near.value = null
            locationViewModel.nearPed.value = null
            locationViewModel.nearBus.value = null
            locationViewModel.nearBusPed.value = null
            pathInfoViewModel.path.value = null
            pathInfoViewModel.start.value = null
            pathInfoViewModel.end.value = null
            pathInfoViewModel.summary.value = null
            findNavController().navigate(R.id.action_Evacuee_to_UserType)
        }

        binding.btnStartNavi.setOnClickListener {
            findNavController().navigate(R.id.action_WithCar_to_Navi)
        }

        binding.rvLocation.run {
            adapter = recyclerViewDataAdaptor
            layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
            setHasFixedSize(true)
            setHorizontalSpace(16.px)
            PagerSnapHelper().attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val position =
                            (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
                                ?: RecyclerView.NO_POSITION
                        onLocationScrolled(position)
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
        requestGpsProviderIfNeeded()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onMapReady(naverMap: NaverMap) {
        map = naverMap
        registerDataChangeListener(map)
        locationViewModel.currentLocation.observeOnce(viewLifecycleOwner) { location ->
            if (location == null) return@observeOnce
            MapHandleUtil.moveMapTo(map, location)

            val latLng = LatLng(location.latitude, location.longitude)
            startMarker = Marker()
            startMarker?.position = latLng
            startMarker?.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
            startMarker?.map = map


            //버스정류장 찾기
            viewLifecycleOwner.lifecycleScope.launch {
                val busStops = busStopFinder.findNearBusStop(latLng.latitude, latLng.longitude)
                Log.i("#BusStop Found", busStops.toString())
                recyclerViewDataAdaptor.updateData(busStops)
                recyclerViewDataAdaptor.notifyDataSetChanged()
            }
            //queryDestWith(location.latitude,location.longitude)

            circleOverlay = CircleOverlay().also {
                it.center = latLng
                it.radius = 1000.0
                it.outlineWidth = 5
                it.outlineColor = Color.RED
                it.color = Color.TRANSPARENT

                it.map = map
            }
        }

        pathInfoViewModel.path.observe(viewLifecycleOwner){
            pathOverlay?.let {
                it.map = null
            }
            pathOverlay = drawPathPolyline(map)
            binding.btnStartNavi.visibility =View.VISIBLE
        }

        getLocationOnce()
    }

    override val destClickFn: (Dest) -> Unit = { dest ->
        pathInfoViewModel.start.postValue(Point(latLng = startMarker?.position, marker = startMarker))
        pathInfoViewModel.end.postValue(Point(latLng = LatLng(dest.lat,dest.lon), dest = dest))

        val cameraTarget = map.cameraPosition.target
        val location = Location("dummy").also {
            it.latitude = startMarker?.position?.latitude ?: cameraTarget.latitude
            it.longitude = startMarker?.position?.longitude ?: cameraTarget.longitude
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val directionResult = getDirectionFinder().findDirection(
                    LatLng(location.latitude, location.longitude),
                    LatLng(dest.lat, dest.lon)
                )
                pathInfoViewModel.path.postValue(directionResult.foundPath.toMutableList())
                pathInfoViewModel.summary.postValue(directionResult)
            } catch (e:Exception) {
                e.message?.let { Log.e("#PinnedSelf", it) }
            }
        }
    }

    fun onLocationScrolled(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            recyclerViewDataAdaptor.getItem(position)?.let { dest ->
                selectedMarker = MapHandleUtil.changeSelectedMarker(selectedMarker,map,dest)
                startMarker?.let { marker ->
                    if(dest.totalTime > 0) return
                    val latitude = marker.position.latitude
                    val longitude = marker.position.longitude
                    viewLifecycleOwner.lifecycleScope.launch {
                        getDirectionFinder().findDirection(
                            LatLng(latitude, longitude),
                            LatLng(dest.lat,dest.lon)
                        ).totalTime?.let { totalTime ->
                            dest.totalTime = totalTime
                        }
                        recyclerViewDataAdaptor.dataChanged()
                    }
                }
            }
        }
    }
}