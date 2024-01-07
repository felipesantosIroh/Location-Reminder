package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var poiMarker: Marker? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val valueZoomInMap = 18f
    private val defaultLocation = LatLng(37.39949343631212, -122.10771245740776)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btSave.setOnClickListener {
            onLocationSelected()
        }
    }

    private fun verifyLocationPermission():Boolean {
        return (ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private val requestPermissionLauncherFineLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted.not()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied_explanation),
                    Toast.LENGTH_LONG
                ).show()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        defaultLocation,
                        valueZoomInMap
                    )
                )
            }
        }

    private fun onLocationSelected() {
        poiMarker?.let {
            _viewModel.reminderSelectedLocationStr.value = poiMarker!!.title
            _viewModel.longitude.value = poiMarker!!.position.longitude
            _viewModel.latitude.value = poiMarker!!.position.latitude
            _viewModel.navigationCommand.value = NavigationCommand.Back
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NONE
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (success.not()) {
                Log.e("SelectLocationFragment", "Could not add style by json")
            }
        } catch (error: Exception) {
            Log.e("SelectLocationFragment", "Could not add style: ${error.message}")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.let {
            map = googleMap
            setMapStyle(map)
            setMapClick(map)
            setPoiClick(map)
            if (verifyLocationPermission()) {
                getUserLocation()
            } else {
                requestPermissionLauncherFineLocation.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            poiMarker?.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            poiMarker?.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLng(poi.latLng))
        }
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { position ->
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                position.latitude,
                position.longitude
            )

            poiMarker?.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .snippet(snippet)
                    .title(getString(R.string.dropped_pin))
            )

            poiMarker?.showInfoWindow()
            map.animateCamera(CameraUpdateFactory.newLatLng(position))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        //Location enabled
        map.isMyLocationEnabled = true

        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_LOW_POWER, 100)
            .setIntervalMillis(1000)
            .setMinUpdateIntervalMillis(500)
            .setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(100)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(requireActivity())

                try {
                    fusedLocationProviderClient.lastLocation
                        .addOnSuccessListener {
                            it?.let {
                                val currentLocation = LatLng(it.latitude, it.longitude)
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        currentLocation,
                                        valueZoomInMap
                                    )
                                )
                            } ?: run {
                                Log.e("SelectLocationFragment",
                                    "Could not get client location"
                                )
                            }
                        }
                } catch (error: Exception) {
                    Log.e(
                        "SelectLocationFragment",
                        "Could not get client location: ${error.message}"
                    )
                }
            }
        }
    }
}