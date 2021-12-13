package kr.ac.korea.oku.emergency.ui.main.evacuee

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
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
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kr.ac.korea.oku.emergency.R
import kr.ac.korea.oku.emergency.data.local.DestinationDataSource
import kr.ac.korea.oku.emergency.data.local.model.Dest
import kr.ac.korea.oku.emergency.databinding.FragmentEvacueeBinding
import kr.ac.korea.oku.emergency.ui.main.evacuee.adapter.EvacueeDestAdaptor
import kr.ac.korea.oku.emergency.ui.main.evacuee.models.ClosestLoc
import kr.ac.korea.oku.emergency.ui.main.locations.DirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.PedPolylineDrawer
import kr.ac.korea.oku.emergency.ui.main.locations.PedestrianDirectionFinder
import kr.ac.korea.oku.emergency.ui.main.locations.PolylineDrawerImpl
import kr.ac.korea.oku.emergency.ui.main.locations.refactor.DistanceCalculator
import kr.ac.korea.oku.emergency.util.gps.CoordCalcUtils
import kr.ac.korea.oku.emergency.util.px
import kr.ac.korea.oku.emergency.util.setHorizontalSpace
import javax.inject.Inject
import kotlin.math.*


@AndroidEntryPoint
class EvacueeFragment : Fragment(), OnMapReadyCallback {

    private var _binding : FragmentEvacueeBinding? = null
    private val binding: FragmentEvacueeBinding
        get() = _binding!!

    @Inject
    lateinit var dataSource: DestinationDataSource
    @Inject
    lateinit var directionFinder : DirectionFinder
    @Inject
    lateinit var pedestrianDirectionFinder: PedestrianDirectionFinder

    private var _view : View? = null
    var fab: FloatingActionButton? = null

    /**
     * Map related status
     */
    private var gpsDialog: AlertDialog? = null
    private var locationEnabled  = false
    private var locationSource : LocationSource? = null
    private val activeMarkers: MutableMap<LatLng, Marker> = mutableMapOf()
    private var currentMarker: Marker? = null

    private var tracker : EvacueeTracker? = null
    private var polylineDrawer : PolylineDrawerImpl? = null
    private var pedPolylineDrawer : PedPolylineDrawer? = null

    lateinit var naverMap: NaverMap
    var polyline: PathOverlay? = null
    var pedPolyline: PathOverlay? = null

    var closestLoc : ClosestLoc? = null
    var pedClosestLoc : ClosestLoc? = null

    var foundPath: MutableList<LatLng>? = null
    var pedFoundPath: MutableList<LatLng>? = null

    var waiting = false

    private val evacueeDestAdaptor : EvacueeDestAdaptor by lazy {
        EvacueeDestAdaptor(findCurrentLocationWith(findDirection()))
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentEvacueeBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //polylineDrawer = PolylineDrawer(this,directionFinder)
//        pedPolylineDrawer = PedPolylineDrawer(this, pedestrianDirectionFinder)

        locationSource = FusedLocationSource(this, 1000)
        _view = view
        binding.mapview.getMapAsync(this)
        fab = view.findViewById(R.id.fab)
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_Evacuee_to_UserType)
        }
        binding.rvLocation.run {
            adapter = evacueeDestAdaptor
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

    private fun onLocationScrolled(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            evacueeDestAdaptor.getItem(position)?.let { moveMap(it) }
        }
    }

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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {

            val lastLocation = locationResult?.lastLocation ?: return
            val coord = LatLng(lastLocation)

            val locationOverlay = naverMap.locationOverlay

            foundPath?.let{
                path ->
                closestLoc?.let {
                    loc ->
                    var nextDistance = CoordCalcUtils.calcDistance(coord.latitude,loc.location.latitude, coord.longitude, loc.location.longitude) * 1000
                    var idx = loc.idx

                    while(true){
                        if(nextDistance < 2) break
                        if(idx + 1 >= path.size) {
                            idx = loc.idx
                            break
                        }
                        val tempLoc = path[idx]
                        nextDistance = CoordCalcUtils.calcDistance(coord.latitude,tempLoc.latitude, coord.longitude, tempLoc.longitude) * 1000
                        idx += 1
                    }

                    if( CoordCalcUtils.calcDistance(coord.latitude,loc.location.latitude, coord.longitude, loc.location.longitude) * 1000 >= nextDistance) {
                        closestLoc = ClosestLoc(path[idx],idx, nextDistance)
                    }

                }
            }

            closestLoc?.let{
                closestLoc ->
                foundPath?.let {
                    path ->
                    val next = closestLoc.idx+1
                    if(next <= path.size) {
                        locationOverlay.bearing = CoordCalcUtils.calculateBearing(closestLoc.location,path[next]).toFloat()
                        locationOverlay.position = closestLoc.location
                        naverMap.cameraPosition = CameraPosition(
                            closestLoc.location,
                            16.0,
                            110.0,
                            CoordCalcUtils.calculateBearing(closestLoc.location,path[next])
                        )
                    }
                }
            }

            if (waiting) {
                waiting = false
                fab?.setImageResource(R.drawable.ic_location_disabled_black_24dp)
                locationOverlay.isVisible = true
            }
        }
    }

    private fun registerDataChangeListener(){
        evacueeDestAdaptor.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val targets = evacueeDestAdaptor.getItemRange(positionStart, positionStart+itemCount)
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

    private fun moveMapTo(location : Location){
        val coord = LatLng(location)
        val locationOverlay = naverMap.locationOverlay
        locationOverlay.position = coord
        locationOverlay.bearing = location.bearing
        locationOverlay.isVisible = true
        naverMap.moveCamera(CameraUpdate.scrollTo(coord))
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

    private fun disableLocation() {
        if (!locationEnabled) {
            return
        }
        naverMap.locationOverlay.isVisible = false
        naverMap.cameraPosition = CameraPosition(
            naverMap.cameraPosition.target,
            naverMap.cameraPosition.zoom,
            0.0,
            naverMap.cameraPosition.bearing)

        view?.let { LocationServices.getFusedLocationProviderClient(it.context).removeLocationUpdates(tracker) }
        locationEnabled = false
    }

    private fun findCurrentLocationWith(invokeTarget : (location:Location, dest: Dest)->Unit ) :
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
                                _view?.findViewById<TextView>(R.id.tvDistance)?.text = "${String.format("%.2f", dest.distance)} Km"
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

    private fun findDestinations(){
        naverMap.locationSource = locationSource
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
                                            Dest(
                                                id = e.id,
                                                name = e.name,
                                                address = e.address,
                                                lat = e.lat,
                                                lon = e.lon,
                                                distance = (DistanceCalculator.calcDistance(location.latitude,e.lat, location.longitude, e.lon)))
                                        }.sortedBy { e2 -> e2.distance }
                                        evacueeDestAdaptor.updateData(converted)
                                        evacueeDestAdaptor.notifyDataSetChanged()
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
                                                        Dest(
                                                            id = e.id,
                                                            name = e.name,
                                                            address = e.address,
                                                            lat = e.lat,
                                                            lon = e.lon,
                                                            distance = (DistanceCalculator.calcDistance(location.latitude,e.lat, location.longitude, e.lon))
                                                        )
                                                    }.sortedBy { e2 -> e2.distance }
                                                    evacueeDestAdaptor.updateData(converted)
                                                    evacueeDestAdaptor.notifyDataSetChanged()
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
                        tracker = EvacueeTracker(this@EvacueeFragment, naverMap)
                        val fused = LocationServices.getFusedLocationProviderClient(view!!.context)
                        fused.requestLocationUpdates(locationRequest, tracker, null)
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

    private fun findDirection() : (location : Location, dest : Dest) -> Unit = {
        loc, dest ->
        //to remove line from map
        polyline?.map = null
        foundPath = null

        pedPolyline?.map = null
        pedFoundPath = null

        //polylineDrawer?.setPolylineWithDirection(loc, dest)
        pedPolylineDrawer?.setPolylineWithDirection(loc, dest)
    }

    companion object {
        private const val LOCATION_REQUEST_INTERVAL = 300
        private val PERMISSIONS = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

}