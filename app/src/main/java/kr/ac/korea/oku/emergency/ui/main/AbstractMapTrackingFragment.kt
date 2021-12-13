package kr.ac.korea.oku.emergency.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import co.kr.tamer.aos.trunk.ui.utils.gps.GpsClient
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.DestinationDataSource
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.data.remote.NaverMapApiService
import kr.ac.korea.oku.emergency.ui.main.destinations.DestinationFinder
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.PathInfoViewModel
import kr.ac.korea.oku.emergency.ui.main.draw.viewmodels.Point
import kr.ac.korea.oku.emergency.ui.main.evacuee.DirectionDrawable
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.DirectionViewModel
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.DistanceCalculator
import javax.inject.Inject

@AndroidEntryPoint
abstract class AbstractMapTrackingFragment : Fragment(), OnMapReadyCallback, DirectionDrawable {
    @Inject
    lateinit var gpsClient: GpsClient
    @Inject
    lateinit var apiService : NaverMapApiService
    @Inject
    lateinit var dataSource: DestinationDataSource
    @Inject
    lateinit var destinationFinder: DestinationFinder

    private var gpsDialog: AlertDialog? = null
    private var locationEnabled  = false
    private val activeMarkers: MutableMap<LatLng, Marker> = mutableMapOf()
    private var currentMarker: Marker? = null
    protected var fab: FloatingActionButton? = null
    protected open var searchDest: Boolean = true
    override lateinit var naverMap: NaverMap
    private var waiting = false

    protected open val drawDirect = false
    protected val viewModel: DirectionViewModel by activityViewModels()
    protected val pathInfoViewModel: PathInfoViewModel by activityViewModels()
    open fun doWhenMapReadyHook() {}

    protected fun onLocationScrolled(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            drawAdaptor.getItem(position)?.let { moveMap(it) }
        }
    }

    protected fun requestGpsProviderIfNeeded() {
        if (!gpsClient.isGpsEnable) {
            openGpsDialog({
                openGpsSetting()
            }, {

            })
        }
    }

    private fun openGpsDialog(
        onPositiveClicked: () -> Unit,
        onNegativeClicked: () -> Unit
    ) {
        if (gpsDialog?.isShowing != true) {
            gpsDialog = context?.let {
                AlertDialog.Builder(it)
                    .setTitle("GPS 켜세요")
                    .setMessage("GPS 켠다")
                    .setPositiveButton("설정") { dialog, which ->
                        onPositiveClicked.invoke()
                        dialog.dismiss()
                    }
                    .setNegativeButton("취소") { dialog, which ->
                        onNegativeClicked.invoke()
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun openGpsSetting() {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).run {
            addCategory(Intent.CATEGORY_DEFAULT)
            startActivity(this)
        }
    }

    override fun findDirection() : (location : Location, dest : Dest) -> Unit = {
            loc, dest ->
        //to remove line from map
        polyline?.map = null
        foundPath = null
        polylineDrawer?.setPolylineWithDirection(loc, dest)
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        //클릭 이벤트는 삭제될 예정
        this.naverMap.setOnMapClickListener { point, latLng ->
            val cameraTarget = this.naverMap.cameraPosition.target
            Toast.makeText(
                this.context,
                "${latLng.latitude}, ${latLng.longitude} : ${cameraTarget.latitude}, ${cameraTarget.longitude}",
                Toast.LENGTH_SHORT
            ).show()
        }

        doWhenMapReadyHook()

        //훅 처럼 뜯어낼 것
        view?.let {
            if(searchDest) {
                startFindDestinations(it)
            }
        }

        fab?.setOnClickListener {
            if (locationEnabled) {
                disableLocation()
                fab?.setImageResource(R.drawable.ic_my_location_black_24dp)
            } else {
                fab?.setImageDrawable(view?.context?.let {
                    CircularProgressDrawable(it).apply {
                        setStyle(CircularProgressDrawable.LARGE)
                        setColorSchemeColors(Color.WHITE)
                    }
                })
                view?.let { it1 -> startLocationTracking(it1) }
            }
        }

        //리사이클 뷰 사용 하는 녀석들에서만 동작하도록 추상화 할것
        registerDataChangeListener()

        //TODO: Extract Class for handle
        if(drawDirect) {
            val cameraTarget = this.naverMap.cameraPosition.target
            val location = Location("dummy").also {
                it.latitude = viewModel.start.value?.latLng?.latitude ?: cameraTarget.latitude
                it.longitude = viewModel.start.value?.latLng?.longitude ?: cameraTarget.longitude
            }
            moveMapTo(location)
            val dest = viewModel.end.value?.dest
            findDirection().invoke(location,dest!!)

            //Add Marker of Start, end
            viewModel.start.value?.latLng?.let { startPosition ->
                val startMarker = Marker().also {
                    it.position = startPosition
                    it.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
                    it.map = this.naverMap
                }
            }

            viewModel.end.value?.latLng?.let { endPosition ->
                val endMarker = Marker().also {
                    it.position = endPosition
                    it.icon = OverlayImage.fromResource(R.drawable.ic_place_red_24dp)
                    it.map = this.naverMap
                }
            }

            //이거 용도가 뭐였지...
            fab?.setImageDrawable(view?.context?.let {
                CircularProgressDrawable(it).apply {
                    setStyle(CircularProgressDrawable.LARGE)
                    setColorSchemeColors(Color.WHITE)
                }
            })

            //현재위치 찾기
            view?.let { it1 -> startLocationTracking(it1) }
        }

    }

    private fun registerDataChangeListener(){
        drawAdaptor.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val targets = drawAdaptor.getItemRange(positionStart, positionStart+itemCount)
                targets.forEach {
                    val latlng =  com.naver.maps.geometry.LatLng(it.lat, it.lon)
                    val marker = Marker()
                    marker.icon = OverlayImage.fromResource(R.drawable.ic_location_24)
                    marker.position = latlng
                    marker.map = naverMap
                    activeMarkers[latlng] = marker
                }
            }
        })
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            Log.i("CallBack", "calleds")
            val lastLocation = locationResult?.lastLocation ?: return
            val coord = LatLng(lastLocation)
            val locationOverlay = naverMap.locationOverlay
            locationOverlay.position = coord
            locationOverlay.bearing = lastLocation.bearing
            naverMap.moveCamera(CameraUpdate.scrollTo(coord))
            if (waiting) {
                waiting = false
                fab?.setImageResource(R.drawable.ic_location_disabled_black_24dp)
                locationOverlay.isVisible = true
            }
        }
    }

    private fun disableLocation() {
        if (!locationEnabled) {
            return
        }
        naverMap.locationOverlay.isVisible = false
        view?.let { LocationServices.getFusedLocationProviderClient(it.context).removeLocationUpdates(locationCallback) }
        locationEnabled = false
    }

    protected fun withCurrentLocation(
        view: View,
        hook: (latitude: Double, longitude: Double) -> Unit
    ) {
        withPermission(view){ doCurrentLocationWithHook(hook) }
    }
    private fun startFindDestinations(view : View){
        withPermission(view){ findDestinations() }
    }
    private fun startLocationTracking(view : View) {
        withPermission(view){ enableLocation() }
    }

    private fun enableLocation() {
        view?.context?.let {
            GoogleApiClient.Builder(it)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    @SuppressLint("MissingPermission")
                    override fun onConnected(bundle: Bundle?) {
                        val locationRequest = LocationRequest().apply {
                            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                            interval = LOCATION_REQUEST_INTERVAL.toLong()
                            fastestInterval = LOCATION_REQUEST_INTERVAL.toLong()
                        }
                        val fused = LocationServices.getFusedLocationProviderClient(view!!.context)
                        fused.requestLocationUpdates(locationRequest, locationCallback, null)
                        locationEnabled = true
                        waiting = true
                    }

                    override fun onConnectionSuspended(i: Int) {
                    }
                })
                .addApi(LocationServices.API)
                .build()
                .connect()
        }
    }
    protected fun findCurrentLocationWith(invokeTarget : (location: Location, dest: Dest)->Unit ) :
                (dest: Dest) -> Unit = {
            dest ->
        view?.context?.let {
            GoogleApiClient.Builder(it)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    @SuppressLint("MissingPermission", "SetTextI18n")
                    override fun onConnected(bundle: Bundle?) {
                        val fused = LocationServices.getFusedLocationProviderClient(view!!.context)
                        fused.lastLocation.addOnSuccessListener {
                                location ->
                            if(location != null){
//                                moveMapTo(location)
                                view?.findViewById<TextView>(R.id.tvDistance)?.text = "${String.format("%.2f", dest.distance)} Km"

                                viewModel.start.postValue(
                                    Point(
                                        latLng = LatLng(location.latitude,location.longitude)
                                    )
                                )
                                viewModel.end.postValue(
                                    Point(
                                        latLng = LatLng(
                                            dest.lat,
                                            dest.lon
                                        ), dest = dest
                                    )
                                )

                                invokeTarget.invoke(location, dest)
                            }else {
                                val locationRequest = LocationRequest().apply {
                                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                    interval = LOCATION_REQUEST_INTERVAL.toLong()
                                    fastestInterval = LOCATION_REQUEST_INTERVAL.toLong()
                                }
                                fused.requestLocationUpdates(locationRequest, object : LocationCallback()
                                {
                                    override fun onLocationResult(locationResult: LocationResult?) {
                                        locationResult ?: return
                                        for( loc in locationResult.locations){
                                            invokeTarget.invoke(loc, dest)
                                            fused.removeLocationUpdates(this)
                                        }
                                    }
                                }, null)
                            }
                        }
                    }
                    override fun onConnectionSuspended(i: Int) {}
                })
                .addApi(LocationServices.API)
                .build()
                .connect()
        }
    }
    protected fun queryDestWith(
        latitude: Double, longitude: Double
    ) {
        activeMarkers.forEach {
            it.value.map = null
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = destinationFinder.findDestinations(latitude,longitude,1)
            drawAdaptor.updateData(result)
            drawAdaptor.notifyDataSetChanged()
        }
    }
    private fun doCurrentLocationWithHook(hook: (latitude: Double, longitude: Double) -> Unit){
        view?.context?.let {
            GoogleApiClient.Builder(it)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    @SuppressLint("MissingPermission")
                    override fun onConnected(bundle: Bundle?) {
                        val fused = LocationServices.getFusedLocationProviderClient(view!!.context)
                        fused.lastLocation.addOnSuccessListener {
                                location ->
                            if(location != null){
                                viewLifecycleOwner.lifecycleScope.launch {
                                    hook.invoke(location.latitude, location.longitude)
                                }
                            }else {
                                val locationRequest = LocationRequest().apply {
                                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                    interval = LOCATION_REQUEST_INTERVAL.toLong()
                                    fastestInterval = LOCATION_REQUEST_INTERVAL.toLong()
                                }
                                fused.requestLocationUpdates(locationRequest, object : LocationCallback()
                                {
                                    override fun onLocationResult(locationResult: LocationResult?) {
                                        locationResult ?: return
                                        for( location in locationResult.locations){
                                            viewLifecycleOwner.lifecycleScope.launch {
                                                hook.invoke(location.latitude, location.longitude)
                                            }
                                            fused.removeLocationUpdates(this)
                                        }
                                    }
                                }, null)
                            }
                        }
                    }
                    override fun onConnectionSuspended(i: Int) {}
                })
                .addApi(LocationServices.API)
                .build()
                .connect()
        }
    }

    /**
     * Old 버전 현재 위치 찾고, 대피소 찾고 recycleedView 업데이트 까지 다 하는 안좋은 코드
     */
    private fun findDestinations(){
        view?.context?.let {
            GoogleApiClient.Builder(it)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    @SuppressLint("MissingPermission")
                    override fun onConnected(bundle: Bundle?) {
                        val fused = LocationServices.getFusedLocationProviderClient(view!!.context)
                        fused.lastLocation.addOnSuccessListener {
                                location ->
                            if(location != null){
                                moveMapTo(location)
                                queryDestWith(location.latitude, location.longitude)
                            }else {
                                val locationRequest = LocationRequest().apply {
                                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                    interval = LOCATION_REQUEST_INTERVAL.toLong()
                                    fastestInterval = LOCATION_REQUEST_INTERVAL.toLong()
                                }
                                fused.requestLocationUpdates(locationRequest, object : LocationCallback()
                                {
                                    override fun onLocationResult(locationResult: LocationResult?) {
                                        locationResult ?: return
                                        for( location in locationResult.locations){
                                            moveMapTo(location)
                                            viewLifecycleOwner.lifecycleScope.launch {
                                                val dao = dataSource.destinationDao()
                                                dao.getAll(location.latitude, location.longitude).collect { result ->
                                                    val converted = result.map { e ->
                                                        Dest(
                                                            id = e.id,
                                                            name = e.name,
                                                            address = e.address,
                                                            lat = e.lat,
                                                            lon = e.lon,
                                                            distance = (DistanceCalculator.calcDistance(location.latitude,e.lat, location.longitude, e.lon)))
                                                    }.filter { dest ->
                                                      dest.distance < 1
                                                    }.sortedBy { e2 ->
                                                        e2.distance
                                                    }
                                                    drawAdaptor.updateData(converted)
                                                    drawAdaptor.notifyDataSetChanged()
                                                }
                                            }
                                            fused.removeLocationUpdates(this)
                                        }
                                    }
                                }, null)
                            }
                        }
                    }
                    override fun onConnectionSuspended(i: Int) {}
                })
                .addApi(LocationServices.API)
                .build()
                .connect()
        }
    }


    private fun moveMap(item: Dest) {
        currentMarker?.let { it.map = null }

        val latLng = com.naver.maps.geometry.LatLng(item.lat, item.lon)
        currentMarker = Marker()
        currentMarker?.position = latLng
        currentMarker?.icon = OverlayImage.fromResource(R.drawable.ic_location_red_24)
        currentMarker?.map = naverMap

        val cameraUpdate = CameraUpdate.scrollTo(latLng).animate(
            CameraAnimation.Easing
        )
        naverMap.moveCamera(cameraUpdate)
    }

    protected fun moveMapTo(location : Location){
        val coord = LatLng(location)
        val locationOverlay = naverMap.locationOverlay
        locationOverlay.position = coord
        locationOverlay.bearing = location.bearing
        locationOverlay.isVisible = true
        naverMap.moveCamera(CameraUpdate.scrollTo(coord))
    }

    protected fun moveMapTo(latitude: Double, longitude: Double){
        val coord = LatLng(latitude, longitude)
        val locationOverlay = naverMap.locationOverlay
        locationOverlay.position = coord
        locationOverlay.isVisible = true
        naverMap.moveCamera(CameraUpdate.scrollTo(coord))
    }

    private fun withPermission(view : View, target : () -> Unit) {
        Dexter.withContext(view.context)
            .withPermissions(PERMISSIONS)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionRationaleShouldBeShown(
                    requests: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report != null) {
                        if (report.areAllPermissionsGranted()) {
                            target()
                        } else {
                            Log.i("PERMISSION", "DENY")
                        }
                    } else {
                        Log.i("PERMISSION", "ERROR")
                    }
                }
            })
            .check()
    }


    companion object {
        private const val TAG = "Fragment"
        private const val LOCATION_REQUEST_INTERVAL = 1000
        private const val PERMISSION_REQUEST_CODE = 100
        private val PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}