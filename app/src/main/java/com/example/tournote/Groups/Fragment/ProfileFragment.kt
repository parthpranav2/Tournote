package com.example.tournote.Groups.Fragment

import android.Manifest
import android.content.Context // Import Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.Onboarding.Activity.LogInActivity
import com.example.tournote.Onboarding.ViewModel.authViewModel
import com.example.tournote.R
import com.example.tournote.Services.LocationTrackingService
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {
    private val viewModel : authViewModel by viewModels()
    private lateinit var databaseRef: DatabaseReference

    // Add this constant for SharedPreferences key
    private val PREF_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        val locationSwitch = view?.findViewById<Switch>(R.id.switch1)

        if (fineLocationGranted || coarseLocationGranted) {
            // Permissions granted, start the service and save preference
            startLocationService()
            locationSwitch?.isChecked = true // Ensure switch is checked
            saveLocationTrackingPreference(true) // Save the state
        } else {
            // Permissions denied, show message and turn off switch, save preference
            Toast.makeText(requireContext(), "Location permissions required for tracking", Toast.LENGTH_SHORT).show()
            locationSwitch?.isChecked = false // Ensure switch is unchecked
            saveLocationTrackingPreference(false) // Save the state
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        databaseRef = FirebaseDatabase.getInstance().getReference("locations")

        val btn = view.findViewById<Button>(R.id.sign_out_button)

        btn.setOnClickListener {
            viewModel.signOut()
        }
        observeModel()

        // Setup location tracking switch
        setupLocationTrackingSwitch(view)

        return view
    }

    // Add onResume to set the switch state when fragment becomes visible
    override fun onResume() {
        super.onResume()
        // Load the preference and set the switch state
        val isTrackingEnabled = loadLocationTrackingPreference()
        view?.findViewById<Switch>(R.id.switch1)?.isChecked = isTrackingEnabled

        // If the switch was previously enabled, ensure the service is running (e.g., if app was killed)
        if (isTrackingEnabled && hasLocationPermissions()) {
            startLocationService()
        } else if (!isTrackingEnabled) {
            stopLocationService() // Ensure service is stopped if preference says it should be
        }
    }




    /**
     * Setup the switch for location tracking service
     */
    private fun setupLocationTrackingSwitch(view: View) {
        val locationSwitch = view.findViewById<Switch>(R.id.switch1)

        // Set the initial state of the switch based on saved preference
        locationSwitch.isChecked = loadLocationTrackingPreference()

        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Switch is ON - Start location tracking
                if (hasLocationPermissions()) {
                    startLocationService()
                    saveLocationTrackingPreference(true) // Save state
                } else {
                    // Request location permissions
                    requestLocationPermissions()
                    // The result of permission request will update the switch state and preference
                }
                set_itIsRefreshing_toTrue()
            } else {
                // Switch is OFF - Stop location tracking
                stopLocationService()
                saveLocationTrackingPreference(false) // Save state
                set_itIsRefreshing_toFalse()
            }
        }
    }


    private fun set_itIsRefreshing_toTrue(){
        val currentUser = GlobalClass.Me?.uid
        databaseRef.child(currentUser?:"null").child("itIsRefreshing").setValue(true)
    }
    private fun set_itIsRefreshing_toFalse(){
        val currentUser = GlobalClass.Me?.uid
        databaseRef.child(currentUser?:"null").child("itIsRefreshing").setValue(false)
    }
    /**
     * Save the location tracking preference to SharedPreferences
     */
    private fun saveLocationTrackingPreference(isEnabled: Boolean) {
        val sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
        with (sharedPrefs.edit()) {
            putBoolean(PREF_LOCATION_TRACKING_ENABLED, isEnabled)
            apply()
        }
    }

    /**
     * Load the location tracking preference from SharedPreferences
     */
    private fun loadLocationTrackingPreference(): Boolean {
        val sharedPrefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean(PREF_LOCATION_TRACKING_ENABLED, false) // Default to false
    }

    /**
     * Check if location permissions are granted
     */
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permissions
     */
    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Start the location tracking service as foreground service
     */
    private fun startLocationService() {
        try {
            val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java)
            serviceIntent.action = LocationTrackingService.ACTION_START_LOCATION_TRACKING

            // Use startForegroundService for Android O and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }

            Toast.makeText(requireContext(), "Location tracking started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start location tracking: ${e.message}", Toast.LENGTH_SHORT).show()
            // Turn off switch and save preference if service failed to start
            view?.findViewById<Switch>(R.id.switch1)?.isChecked = false
            saveLocationTrackingPreference(false)
        }
    }

    /**
     * Stop the location tracking service
     */
    private fun stopLocationService() {
        try {
            val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java)
            serviceIntent.action = LocationTrackingService.ACTION_STOP_LOCATION_TRACKING
            requireContext().startService(serviceIntent)
            Toast.makeText(requireContext(), "Location tracking stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to stop location tracking: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }






    private fun observeModel(){

        viewModel.loginError.observe(viewLifecycleOwner)
        { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.toastmsg.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner) {
            if (it) {
                // Stop location service and save preference when logging out
                stopLocationService()
                saveLocationTrackingPreference(false) // Ensure preference is off on logout

                val intent = Intent(requireContext(), LogInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
                viewModel.clearNavigationLogin()
            }
        }

        viewModel.navigateToMain.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                val intent = Intent(requireContext(), GroupSelectorActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish() // 👈 kills the hosting activity so it's not in the back stack
                viewModel.clearRoleLoadingMain()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Optional: Stop service when fragment is destroyed
        // Uncomment the line below if you want to stop tracking when leaving this fragment
        // stopLocationService()
    }
}