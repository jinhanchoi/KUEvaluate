package kr.ac.korea.oku.emergency.ui.main.driver

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.DestinationDataSource
import kr.ac.korea.oku.emergency.data.local.model.Destination
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.databinding.FragmentDriverBinding
import kr.ac.korea.oku.emergency.ui.main.driver.adapter.DestAdaptor
import kr.ac.korea.oku.emergency.util.px
import javax.inject.Inject

@AndroidEntryPoint
class DriverFragment : Fragment(), OnMapReadyCallback {
    val APIKEY_ID = "kio62awlhg"
    val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"
    @Inject
    lateinit var apiService : NaverMapApiService
    @Inject
    lateinit var dataSource: DestinationDataSource

    private lateinit var naverMap: NaverMap
    private val activeMarkers: MutableList<Marker> = mutableListOf()
    private var _binding: FragmentDriverBinding? = null
    private val binding: FragmentDriverBinding
        get() = _binding!!

    private val destAdaptor : DestAdaptor by lazy {
        DestAdaptor()
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        destAdaptor.items.forEach { dest ->
            val marker = Marker()
            marker.icon = OverlayImage.fromResource(R.drawable.ic_location_24)
            marker.position = com.naver.maps.geometry.LatLng(dest.lat, dest.lon)
            marker.map = naverMap
            activeMarkers.add(marker)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        Executors.newSingleThreadExecutor().execute {
////            val call = apiService.requestPath(APIKEY_ID, APIKEY,"129.089441, 35.231100", "129.084454, 35.228982")
////            val res = call.execute()
////            println(res.body())
//
//        }
        return FragmentDriverBinding.inflate(inflater, container, false).also {
            _binding = it
            _binding?.mapview?.onCreate(savedInstanceState)
        }.root
    }

    override fun onResume() {
        super.onResume()
        binding.mapview.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapview.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_Driver_to_UserType)
        }

        binding.rvLocation.run {
            adapter = destAdaptor
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

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = dataSource.destinationDao()
            dao.getAll(37.505890, 126.888667).collect {
                destAdaptor.updateData(it)
                destAdaptor.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onLocationScrolled(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            destAdaptor.getItem(position)?.let { moveMap(it) }
        }
    }

    private fun moveMap(item: Destination) {
        val cameraUpdate = CameraUpdate.scrollTo(com.naver.maps.geometry.LatLng(item.lat, item.lon)).animate(
            CameraAnimation.Easing
        )
        naverMap?.moveCamera(cameraUpdate)
    }

}

fun RecyclerView.setVerticalSpace(space: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                outRect.bottom = space
            }
        }
    })
}

fun RecyclerView.setHorizontalSpace(space: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                outRect.right = space
            }
        }
    })
}