package kr.ac.korea.oku.emergency.ui.main.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
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
import kr.ac.korea.oku.emergency.databinding.FragmentDriverBinding
import kr.ac.korea.oku.emergency.ui.main.driver.adapter.DestAdaptor
import kr.ac.korea.oku.emergency.util.px
import kr.ac.korea.oku.emergency.util.setHorizontalSpace
import javax.inject.Inject


@AndroidEntryPoint
class DriverFragment : Fragment(), OnMapReadyCallback {
    val APIKEY_ID = "kio62awlhg"
    val APIKEY = "qvNDNWz3EKLRea4JlkUIDWRiLdO27ODpkzvtadT1"
    @Inject
    lateinit var gpsClient: GpsClient
    @Inject
    lateinit var apiService : NaverMapApiService
    @Inject
    lateinit var dataSource: DestinationDataSource

    private var gpsDialog: AlertDialog? = null
    private lateinit var naverMap: NaverMap
    private var fab: FloatingActionButton? = null
    private var locationEnabled  = false

    private val activeMarkers: MutableList<Marker> = mutableListOf()
    private var _binding: FragmentDriverBinding? = null
    private val binding: FragmentDriverBinding
        get() = _binding!!

    private val destAdaptor : DestAdaptor by lazy {
        DestAdaptor()
    }
    private var waiting = false

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        view?.let { startFindDestinations(it) }
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

        registerDataChangeListener()
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
        requestGpsProviderIfNeeded()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapview.getMapAsync(this)
        fab = view.findViewById(R.id.fab)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun registerDataChangeListener(){
        destAdaptor.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val targets = destAdaptor.getItemRange(positionStart, positionStart+itemCount)
                targets.forEach {
                    val marker = Marker()
                    marker.icon = OverlayImage.fromResource(R.drawable.ic_location_24)
                    marker.position = com.naver.maps.geometry.LatLng(it.lat, it.lon)
                    marker.map = naverMap
                    activeMarkers.add(marker)
                }
            }
        })
    }

    private fun onLocationScrolled(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            destAdaptor.getItem(position)?.let { moveMap(it) }
        }
    }

    private fun moveMap(item: Dest) {
        val cameraUpdate = CameraUpdate.scrollTo(com.naver.maps.geometry.LatLng(item.lat, item.lon)).animate(
            CameraAnimation.Easing
        )
        naverMap.moveCamera(cameraUpdate)
    }


    private fun requestGpsProviderIfNeeded() {
        if (!gpsClient.isGpsEnable) {
            openGpsDialog({
                openGpsSetting()
            }, {

            })
        }
    }

    private fun startFindDestinations(view : View){
        withPermission(view){ findDestinations() }
    }
    private fun startLocationTracking(view : View) {
        withPermission(view){ enableLocation() }
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
    private fun moveMapTo(location : Location){
        val coord = LatLng(location)
        val locationOverlay = naverMap.locationOverlay
        locationOverlay.position = coord
        locationOverlay.bearing = location.bearing
        locationOverlay.isVisible = true
        naverMap.moveCamera(CameraUpdate.scrollTo(coord))
    }
    private fun disableLocation() {
        if (!locationEnabled) {
            return
        }
        naverMap.locationOverlay.isVisible = false
        view?.let { LocationServices.getFusedLocationProviderClient(it.context).removeLocationUpdates(locationCallback) }
        locationEnabled = false
    }
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
                                viewLifecycleOwner.lifecycleScope.launch {
                                    val dao = dataSource.destinationDao()
                                    dao.getAll(location.latitude, location.longitude).collect { result ->
                                        val converted = result.map { e ->
                                            Dest(e.id, e.name,e.address, e.lat, e.lon, (calcDistance(location.latitude,e.lat, location.longitude, e.lon) * 1000).toInt())
                                        }.sortedBy { e2 -> e2.distance }
                                        destAdaptor.updateData(converted)
                                        destAdaptor.notifyDataSetChanged()
                                    }
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
                                            moveMapTo(location)
                                            viewLifecycleOwner.lifecycleScope.launch {
                                                val dao = dataSource.destinationDao()
                                                dao.getAll(location.latitude, location.longitude).collect { result ->
                                                    val converted = result.map { e ->
                                                        Dest(e.id, e.name,e.address, e.lat, e.lon, (calcDistance(location.latitude,e.lat, location.longitude, e.lon) * 1000).toInt())
                                                    }.sortedBy { e2 -> e2.distance }
                                                    destAdaptor.updateData(converted)
                                                    destAdaptor.notifyDataSetChanged()
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

    private fun calcDistance(orglat1 : Double, orglat2: Double, orglon1 : Double, orglon2 : Double) : Double{
        val lon1 = Math.toRadians(orglon1)
        val lon2 = Math.toRadians(orglon2)
        val lat1 = Math.toRadians(orglat1)
        val lat2 = Math.toRadians(orglat2)

        val dlon: Double = lon2 - lon1
        val dlat: Double = lat2 - lat1
        val a = (Math.pow(Math.sin(dlat / 2), 2.0)
                + (Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2), 2.0)))

        val c = 2 * Math.asin(Math.sqrt(a))
        val r = 6371.0
        return c * r
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