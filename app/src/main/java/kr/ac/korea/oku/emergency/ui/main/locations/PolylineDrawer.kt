package kr.ac.korea.oku.emergency.ui.main.locations

import android.location.Location
import kotlinx.coroutines.Job
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.ui.main.evacuee.DirectionDrawable

interface PolylineDrawer {
    val caller : DirectionDrawable
    val finder : DirectionsFinder
    fun setPolylineWithDirection(loc: Location, dest: Dest): Job
}