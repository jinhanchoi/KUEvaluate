package kr.ac.korea.oku.emergency.ui.main.draw.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naver.maps.geometry.LatLng

class PathInfoViewModel : ViewModel(){
    val start = MutableLiveData<Point>()
    val end = MutableLiveData<Point>()
    val path = MutableLiveData<MutableList<LatLng>>()
    val summary = MutableLiveData<Path>()
}