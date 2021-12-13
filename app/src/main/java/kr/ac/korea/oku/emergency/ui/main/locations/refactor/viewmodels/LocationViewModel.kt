package kr.ac.korea.oku.emergency.ui.main.locations.refactor.viewmodels

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Path

class LocationViewModel : ViewModel() {
    val currentLocation = MutableLiveData<Location>()
    val near = MutableLiveData<Path>()
    val nearPed = MutableLiveData<Path>()
    val nearBus = MutableLiveData<Path>()
    val nearBusPed = MutableLiveData<Path>()
}