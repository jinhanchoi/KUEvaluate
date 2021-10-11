package co.kr.tamer.aos.trunk.ui.utils.gps

import android.location.Location

typealias OnLocationChanged = (Location) -> Unit
typealias OnPermissionDenied = () -> Unit
typealias OnPermissionError = () -> Unit