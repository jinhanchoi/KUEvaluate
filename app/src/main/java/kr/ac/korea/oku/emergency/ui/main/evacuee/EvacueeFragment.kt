package kr.ac.korea.oku.emergency.ui.main.evacuee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kr.ac.korea.oku.emergency.databinding.FragmentEvacueeBinding

class EvacueeFragment : Fragment() {
    private var _binding : FragmentEvacueeBinding? = null
    private val binding: FragmentEvacueeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentEvacueeBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}