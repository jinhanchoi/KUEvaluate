package kr.ac.korea.oku.emergency.ui.main.evacuee.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentSelflocselectBinding
import kr.ac.korea.oku.emergency.ui.main.AbstractTrackableFragement
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.DirectionViewModel
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTracker
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.LocationTrackerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels.LocationViewModel

/**
 * 자체대피 위치 선택
 */
@AndroidEntryPoint
class SelfLocSelectFragment : AbstractTrackableFragement(){

    private var _binding : FragmentSelflocselectBinding? = null
    private val binding: FragmentSelflocselectBinding
        get() = _binding!!

    override val locationTracker : LocationTracker = LocationTrackerImpl()
    override val locationViewModel: LocationViewModel by activityViewModels()

    private val viewModel: DirectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSelflocselectBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            locationViewModel.currentLocation.value = null
            locationViewModel.near.value = null
            locationViewModel.nearPed.value = null
            locationViewModel.nearBus.value = null
            locationViewModel.nearBusPed.value = null
            findNavController().navigate(R.id.action_SelfLoc_to_UserType_Selection)
        }
        binding.btnSelfEvacuatePin.setOnClickListener {
            findNavController().navigate(R.id.action_SelfLoc_to_Pin)
        }
        binding.btnSelfEvacuateGPS.setOnClickListener{
            getLocationOnce()
            findNavController().navigate(R.id.action_SelfLoc_to_GPS)
        }
    }

    override fun onResume() {
        super.onResume()
        println(viewModel.directions.value?.size)
    }
}