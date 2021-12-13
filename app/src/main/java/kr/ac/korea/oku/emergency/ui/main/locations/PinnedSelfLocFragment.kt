package kr.ac.korea.oku.emergency.ui.main.locations

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentPinnedSelflocBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.MapHandleUtil
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PinnedSelfLocFragment : AbstractTrackableFragement(), OnMapReadyCallback {
    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()

    lateinit var map: NaverMap
    private var _binding : FragmentPinnedSelflocBinding? = null
    private val binding: FragmentPinnedSelflocBinding
        get() = _binding!!

    @Inject
    lateinit var directionsFinder: DirectionFinder

    override fun hasDestAdaptor(): Boolean = false
    /**
     * Map related status
     */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPinnedSelflocBinding.inflate(inflater, container, false).also {
            _binding = it
            _binding?.mapview?.onCreate(savedInstanceState)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        binding.backBtn.setOnClickListener {
            locationViewModel.currentLocation.postValue(null)
            findNavController().navigate(R.id.action_Pinned_to_SelfLocSelect)
        }

        binding.btnLocSelect.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                binding.marker.visibility = View.INVISIBLE

                //위치 지정
                val cameraTarget = map.cameraPosition.target
                val latLng = LatLng(cameraTarget.latitude, cameraTarget.longitude)
                val location = Location("dummy").also {
                    it.latitude = latLng.latitude
                    it.longitude = latLng.longitude
                }
                locationViewModel.currentLocation.postValue(location)
                findNavController().navigate(R.id.action_SelfLocSelect_to_TypeSelect)
            } else {
                binding.marker.visibility = View.VISIBLE
                binding.btnLocSelect.visibility = View.VISIBLE
            }
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

    override fun onMapReady(naverMap: NaverMap) {
        map = naverMap
        registerDataChangeListener(map)
        locationViewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            if(location == null) return@observe
            MapHandleUtil.moveMapTo(map,location)
        }
        getLocationOnce()
    }
}