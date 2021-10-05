package kr.ac.korea.oku.emergency.ui.main.driver

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.CoroutinesRoom.Companion.execute
import dagger.hilt.android.AndroidEntryPoint
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.databinding.FragmentDriverBinding
import kr.ac.korea.oku.emergency.ui.main.driver.adapter.DestAdaptor
import kr.ac.korea.oku.emergency.util.px
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class DriverFragment : Fragment() {
    val APIKEY_ID = "kio62awlhg"
    val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"
    @Inject
    lateinit var apiService : NaverMapApiService
    private var _binding: FragmentDriverBinding? = null
    private val binding: FragmentDriverBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Executors.newSingleThreadExecutor().execute {
            val call = apiService.requestPath(APIKEY_ID, APIKEY,"129.089441, 35.231100", "129.084454, 35.228982")
            val res = call.execute()
            println(res.body())
        }

        return FragmentDriverBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_Driver_to_UserType)
        }

        binding.rvLocation.run {
            adapter = DestAdaptor()
            layoutManager = LinearLayoutManager(view.context)
            setVerticalSpace(24.px)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

fun RecyclerView.setVerticalSpace(space: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                outRect.bottom = space
            }
        }
    })
}