package kr.ac.korea.oku.emergency.ui.main.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback

class DriverMapFragment : Fragment(), OnMapReadyCallback {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onMapReady(naverMap: NaverMap) {
        TODO("Not yet implemented")
    }
}