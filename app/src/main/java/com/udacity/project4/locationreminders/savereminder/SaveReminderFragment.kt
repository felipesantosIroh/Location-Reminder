package com.udacity.project4.locationreminders.savereminder

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "action.ACTION_GEOFENCE_EVENT"
    }

    private lateinit var geofenceClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        geofenceClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if(_viewModel.validateEnteredData(reminderDataItem)){
                handledLocationsPermissionsToSaveGeo()
            }
        }
    }

    private fun verifyPermission(permission: String):Boolean {
        return when(permission) {
            POST_NOTIFICATIONS -> {
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU).not() ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED)
            }
            ACCESS_BACKGROUND_LOCATION -> {
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q).not() ||
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> (ActivityCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun showPermissionDenied(stringId: Int){
        Toast.makeText(requireContext(), getString(stringId), Toast.LENGTH_LONG).show()
    }

    private fun handledLocationsPermissionsToSaveGeo(){
        if (verifyPermission(ACCESS_FINE_LOCATION)) {
            if (verifyPermission(ACCESS_BACKGROUND_LOCATION)) {
                if (verifyPermission(POST_NOTIFICATIONS)){
                    checkDeviceLocationAndStartGeofence()
                } else {
                    requestPermissionLauncherPostNotification.launch(POST_NOTIFICATIONS)
                }
            } else {
                requestPermissionLauncherBackgroundLocation.launch(ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            requestPermissionLauncherFineLocation.launch(ACCESS_FINE_LOCATION)
        }
    }


    private val requestPermissionLauncherFineLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (verifyPermission(ACCESS_BACKGROUND_LOCATION).not()){
                    requestPermissionLauncherBackgroundLocation.launch(ACCESS_BACKGROUND_LOCATION)
                } else {
                    if (verifyPermission(POST_NOTIFICATIONS).not()){
                        requestPermissionLauncherPostNotification.launch(POST_NOTIFICATIONS)
                    }
                }
            } else {
                showPermissionDenied(R.string.location_required_error)
            }
        }

    private val requestPermissionLauncherBackgroundLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted.not()) {
                if (verifyPermission(POST_NOTIFICATIONS).not()){
                    requestPermissionLauncherPostNotification.launch(POST_NOTIFICATIONS)
                }
            } else {
                showPermissionDenied(R.string.location_required_error)
            }
        }

    private fun showShouldRequestPostNotificationDialog(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_necessary))
            .setMessage(R.string.permission_necessary_message)
            .setPositiveButton(
                getString(R.string.ok)
            ) { dialog, _ ->
                requestPermissionLauncherPostNotification.launch(POST_NOTIFICATIONS)
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                showPermissionDenied(R.string.location_required_error)
                dialog.dismiss()
            }
            .show()
    }

    private val requestPermissionLauncherPostNotification =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                if (shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)){
                    showShouldRequestPostNotificationDialog()
                } else {
                    showPermissionDenied(R.string.location_required_error)
                }
            }
        }

    private val requestSettingsLauncherLocation =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == RESULT_OK){
                saveGeofenceToReminder()
            }
        }

    private fun checkDeviceLocationAndStartGeofence() {
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
        locationSettingsResponseTask.addOnFailureListener { error ->
            if (error is ResolvableApiException) {
                try {
                    showPermissionDenied(R.string.permission_denied_explanation)
                    requestSettingsLauncherLocation.launch(
                        IntentSenderRequest.Builder(
                            error.resolution
                        ).build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("SaveReminderFragment", "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful){
                saveGeofenceToReminder()
            }
        }
    }

    private fun saveGeofenceToReminder() {
        _viewModel.validateAndSaveReminder(reminderDataItem)
        val geofence = geofenceBuilder(
            reminderDataItem.latitude!!,
            reminderDataItem.longitude!!,
            reminderDataItem.id
        )
        val geofenceRequest = geofenceRequest(geofence)
        addGeofence(geofenceRequest, geofencePendingIntent)
        _viewModel.onClear()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent =
            Intent(requireActivity().applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun geofenceRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .addGeofence(geofence)
            .build()
    }

    private fun geofenceBuilder(latitude: Double, longitude: Double, id: String): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, 100f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }


    @SuppressLint("MissingPermission")
    private fun addGeofence(
        geofenceRequest: GeofencingRequest,
        geofencePendingIntent: PendingIntent
    ) {
        geofenceClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.validateEnteredData(reminderDataItem)
                Log.i(
                    "SaveReminderFragment",
                    "Added Geofence"
                )
            }
            addOnFailureListener { exception ->
                exception.message?.let {
                    Log.e(
                        "SaveReminderFragment",
                        "Could not add geofence ${exception.message}"
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}