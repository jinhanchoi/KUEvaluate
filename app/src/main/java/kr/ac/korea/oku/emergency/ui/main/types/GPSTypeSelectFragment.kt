package kr.ac.korea.oku.emergency.ui.main.types

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentGpsTypeselectBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.locations.BusStopFinder
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.PedestrianDirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import kr.ac.korea.oku.emergency.util.LiveDataExtension.observeOnce
import javax.inject.Inject

@AndroidEntryPoint
class GPSTypeSelectFragment : AbstractTrackableFragement(){

    private var _binding : FragmentGpsTypeselectBinding? = null
    private val binding: FragmentGpsTypeselectBinding
        get() = _binding!!

    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()

    @Inject
    lateinit var directionsFinder: DirectionFinder
    @Inject
    lateinit var pedDirectionFinder: PedestrianDirectionFinder

    @Inject
    lateinit var busStopFinder: BusStopFinder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentGpsTypeselectBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getLocationOnce()

        locationViewModel.near.observe(viewLifecycleOwner){ path ->
            if(path == null) return@observe
            binding.tvSelfNearInfo.text = "자동차: ${path.totalTime?.div(1000)?.div(60)} 분"
        }
        locationViewModel.nearPed.observe(viewLifecycleOwner){ path ->
            if(path == null) return@observe
            binding.tvSelfNearPedInfo.text = "도보: ${path.totalTime?.div(60)} 분"
        }

        locationViewModel.nearBus.observe(viewLifecycleOwner){ path ->
            if(path == null) return@observe
            binding.tvWithCarNearInfo.text = "자동차: ${path.totalTime?.div(1000)?.div(60)} 분"
        }
        locationViewModel.nearBusPed.observe(viewLifecycleOwner){ path ->
            if(path == null) return@observe
            binding.tvWithCarNearPedInfo.text = "도보: ${path.totalTime?.div(60)} 분"
        }

       //현재 위치 구하기
        locationViewModel.currentLocation.observeOnce(viewLifecycleOwner){ location ->
            if(location == null) return@observeOnce
            val latLng = LatLng(location.latitude, location.longitude)
            //버스정류장 찾기
            viewLifecycleOwner.lifecycleScope.launch {

                launch {
                    //대피소 찾기
                    val dest = destinationFinder.findDestinations(
                        to = 3,
                        latitude = latLng.latitude,
                        longitude = latLng.longitude
                    )
                    if(dest.isEmpty()){
                        binding.tvSelfNearInfo.text = "자동차: 없음"
                        binding.tvSelfNearPedInfo.text = "도보: 없음"
                        return@launch
                    }

                    dest.let {
                        Log.i("# Pinned-Self", "$dest")
                    }
                    dest.get(0).let {
                        val to = LatLng(it.lat, it.lon)
                        val nearPed = pedDirectionFinder.findDirection(latLng, to)
                        val near = directionsFinder.findDirection(latLng, to)
                        locationViewModel.nearPed.postValue(nearPed)
                        locationViewModel.near.postValue(near)
                    }
                }

                launch {
                    val busStops = busStopFinder.findNearBusStop(latLng.latitude, latLng.longitude)
                    Log.i("#BusStop Found", busStops.toString())

                    if(busStops.isEmpty()) {
                        binding.tvWithCarNearInfo.text = "자동차: 없음"
                        binding.tvWithCarNearPedInfo.text = "도보: 없음"
                        return@launch
                    }
                    busStops.get(0).let {
                        val to = LatLng(it.lat,it.lon)
                        val bearBusPed = pedDirectionFinder.findDirection(latLng, to)
                        val nearBus = directionsFinder.findDirection(latLng, to)
                        locationViewModel.nearBusPed.postValue(bearBusPed)
                        locationViewModel.nearBus.postValue(nearBus)
                    }

                }
            }
        }

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_Selection_to_Self_Loc)
        }
        binding.btnSelfEvacuate.setOnClickListener {
            findNavController().navigate(R.id.action_Type_to_SelfEvac)
        }
        binding.btnWithCar.setOnClickListener{
            findNavController().navigate(R.id.action_Type_to_WithCar)
        }
    }

    override fun onDestroyView() {
        binding.tvSelfNearInfo.text = ""
        binding.tvSelfNearPedInfo.text = ""
        binding.tvWithCarNearInfo.text = ""
        binding.tvWithCarNearPedInfo.text = ""
        super.onDestroyView()
        _binding = null

    }
}