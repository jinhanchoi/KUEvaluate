package kr.ac.korea.oku.emergency.ui.main.evacuee.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.naver.maps.geometry.LatLng
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Point

class DirectionViewModel : ViewModel(){
    val start = MutableLiveData<Point>()
    val end = MutableLiveData<Point>()
    val directions = MutableLiveData<MutableList<LatLng>>()
    val pathResult = MutableLiveData<Path>()
}