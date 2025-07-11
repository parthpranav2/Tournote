package com.example.tournote.Functionality.Segments.TrackFriends

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.tournote.R
import android.util.Log
import android.webkit.WebChromeClient
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.tournote.GlobalClass
import com.example.tournote.databinding.FragmentTrackFriendsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackFriendsFragment : Fragment() {

    private lateinit var binding: FragmentTrackFriendsBinding
    private val viewModel: TrackFriendsViewModel by activityViewModels()
    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var cancellationTokenSource: CancellationTokenSource? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val locationPermissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackFriendsBinding.inflate(inflater, container, false)

        if(GlobalClass.GroupDetails_Everything.isGroupValid==false){
            binding.relGroupInvalid.visibility=View.VISIBLE
        }else{
            binding.relGroupInvalid.visibility=View.GONE

            webView = binding.WebView
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            setupLocationRequest()
            setupWebView()
            setupObservers()
            setupClickListeners()
        }

        return binding.root
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000L)
            .setMaxUpdateDelayMillis(15000L)
            .build()
    }

    private fun setupObservers() {
        viewModel.showPermissionRequest.observe(viewLifecycleOwner) { showPermission ->
            binding.relPermissions.visibility = if (showPermission) View.VISIBLE else View.GONE
        }

        viewModel.showMapView.observe(viewLifecycleOwner) { showMap ->
            binding.relWebView.visibility = if (showMap) View.VISIBLE else View.GONE
            if (showMap) {
                requestLocationPermission()
            }
        }

        viewModel.isWebViewReady.observe(viewLifecycleOwner) { isReady ->
            if (isReady) {
                // WebView is ready, check if there are pending location updates
                viewModel.currentLocation.value?.let { location ->
                    if (viewModel.locationUpdatePending.value == true) {
                        // Location update will be handled by the webview command observer
                    }
                }
            }
        }

        viewModel.webViewCommand.observe(viewLifecycleOwner) { command ->
            command?.let {
                executeWebViewCommand(it)
                viewModel.clearWebViewCommand()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnPermission.setOnClickListener {
            viewModel.onPermissionGranted()
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.onWebViewPageFinished()
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                viewModel.onWebViewError(description)

                // Retry loading after a delay
                mainHandler.postDelayed({
                    if (isAdded && !isDetached) {
                        webView.loadUrl("file:///android_asset/track_friends_map.html")
                    }
                }, 2000)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.apply {
                    Log.d("WebViewConsole", "${message()} -- From ${sourceId()}:${lineNumber()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        webView.loadUrl("file:///android_asset/track_friends_map.html")
    }

    private fun executeWebViewCommand(command: TrackFriendsViewModel.WebViewCommand) {
        val javascript = when (command) {
            is TrackFriendsViewModel.WebViewCommand.UpdateUserLocation -> {
                """
                    if (typeof setUserLocation === 'function') {
                        setUserLocation(${command.latitude}, ${command.longitude}, '${command.profilePicUrl}');
                    } else {
                        console.log('setUserLocation function not available yet');
                    }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.AddFriendMarker -> {
                """
                    if (typeof addFriendMarker === 'function') {
                        addFriendMarker(${command.id}, '${command.name}', ${command.lat}, ${command.lng}, '${command.status}', '${command.profilePicUrl}');
                    }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.UpdateFriendLocation -> {
                """
                    if (typeof updateFriendLocation === 'function') {
                        updateFriendLocation(${command.id}, ${command.lat}, ${command.lng}, '${command.status}', '${command.profilePicUrl}');
                    }
                """.trimIndent()
            }
            is TrackFriendsViewModel.WebViewCommand.RemoveFriendMarker -> {
                """
                    if (typeof removeFriendMarker === 'function') {
                        removeFriendMarker(${command.id});
                    }
                """.trimIndent()
            }
        }

        webView.evaluateJavascript(javascript, object : ValueCallback<String> {
            override fun onReceiveValue(result: String?) {
                Log.d("WebViewJS", "Command executed: $result")
            }
        })
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d("TrackFriendsFragment", "Location permission rationale needed")
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermissionRequestCode
                )
            }
            else -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    locationPermissionRequestCode
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (!checkLocationPermission()) {
            Log.w("TrackFriendsFragment", "Location permission not granted")
            return
        }

        // Cancel any existing location request
        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        // Try to get last known location first for faster response
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null && viewModel.isLocationRecent(lastLocation)) {
                Log.d("TrackFriendsFragment", "Using last known location")
                viewModel.updateCurrentLocation(lastLocation)
            } else {
                Log.d("TrackFriendsFragment", "Last known location is null or too old, getting current location")
                requestCurrentLocationWithRetry()
            }
        }.addOnFailureListener {
            Log.w("TrackFriendsFragment", "Failed to get last known location, getting current location")
            requestCurrentLocationWithRetry()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestCurrentLocationWithRetry() {
        if (!viewModel.shouldRetryLocation()) {
            Log.e("TrackFriendsFragment", "Max location retries reached")
            startLocationUpdates() // Fall back to location updates
            return
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("TrackFriendsFragment", "Current location obtained: ${it.latitude}, ${it.longitude}")
                viewModel.updateCurrentLocation(it)
            } ?: run {
                Log.w("TrackFriendsFragment", "Current location is null, retrying...")
                if (viewModel.shouldRetryLocation()) {
                    lifecycleScope.launch {
                        delay(2000)
                        requestCurrentLocationWithRetry()
                    }
                } else {
                    startLocationUpdates()
                }
            }
        }.addOnFailureListener { exception ->
            viewModel.onLocationUpdateFailed(exception)
            if (viewModel.shouldRetryLocation()) {
                lifecycleScope.launch {
                    delay(2000)
                    requestCurrentLocationWithRetry()
                }
            } else {
                startLocationUpdates()
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        if (!checkLocationPermission()) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    Log.d("TrackFriendsFragment", "Location update: ${location.latitude}, ${location.longitude}")
                    viewModel.updateCurrentLocation(location)
                    // Stop location updates after getting first location
                    stopLocationUpdates()
                }
            }
        }

        locationRequest?.let { request ->
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onLocationPermissionGranted()
                getCurrentLocation()
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermission() && viewModel.isWebViewReady.value == true) {
            getCurrentLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        cancellationTokenSource?.cancel()
        webView.destroy()
    }

    // Public methods for external access (if needed)
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        viewModel.addFriendOnMap(id, name, lat, lng, status, profilePicUrl)
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        viewModel.updateFriendOnMap(id, lat, lng, status, profilePicUrl)
    }

    fun removeFriendFromMap(id: Int) {
        viewModel.removeFriendFromMap(id)
    }

    // Under progress - keeping as is
    private fun showAuthByEmailPassBottomSheet() {
        val dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bsfragment_emailpass)
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }
        val txtEnteredPass = dialog.findViewById<TextView>(R.id.txtEnteredPass)
        val btnConfirm = dialog.findViewById<RelativeLayout>(R.id.btnCnfrm)
    }
}