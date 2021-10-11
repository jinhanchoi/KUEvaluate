package kr.ac.korea.oku.emergency.ui.main.evacuee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentEvacueeBinding

@AndroidEntryPoint
class EvacueeFragment : Fragment(), OnMapReadyCallback {
    private var _binding : FragmentEvacueeBinding? = null
    private val binding: FragmentEvacueeBinding
        get() = _binding!!

    private lateinit var naverMap: NaverMap
    private var fab: FloatingActionButton? = null
    private var locationEnabled  = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentEvacueeBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        fab = view.findViewById(R.id.fab)
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_Evacuee_to_UserType)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(naverMap : NaverMap) {
        this.naverMap = naverMap
    }
}