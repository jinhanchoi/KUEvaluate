package kr.ac.korea.oku.emergency.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.databinding.FragmentUsertypeBinding

@AndroidEntryPoint
class UserTypeSelectionFragment : Fragment() {
    private var _binding: FragmentUsertypeBinding? = null
    private val binding: FragmentUsertypeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentUsertypeBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_UserType_to_Driver)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}