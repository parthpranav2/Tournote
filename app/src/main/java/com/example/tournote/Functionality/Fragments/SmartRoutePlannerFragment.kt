package com.example.tournote.Functionality.Fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.Functionality.GeocodingResultsAdapter
import com.example.tournote.Functionality.GeocodingResultsDataClass
import com.example.tournote.R
import com.example.tournote.databinding.FragmentSmartRoutePlannerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException


class SmartRoutePlannerFragment : Fragment() {

    private var _binding: FragmentSmartRoutePlannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Start point properties
    var startName: String = "Your Current Location" // Default start name
    var startLatitude: Double = 28.6139 // Default to New Delhi
    var startLongitude: Double = 77.2090

    // Stop point properties
    var stopName: String = "" // Default empty stop name
    var stopLatitude: Double = 0.0
    var stopLongitude: Double = 0.0
    var hasStop: Boolean = false // Track if stop is added

    // Destination properties
    var destinationName: String = "Taloda" // Default to Taloda
    var destinationLatitude: Double = 21.5628
    var destinationLongitude: Double = 74.2135

    // Search related
    private lateinit var geocodingResultsAdapter: GeocodingResultsAdapter
    private val geocodingResultsList = mutableListOf<GeocodingResultsDataClass>()
    private val searchScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null
    private val httpClient = OkHttpClient()

    // Enum to keep track of which field is currently being searched/edited
    private var currentSearchTarget: SearchTarget = SearchTarget.NONE

    enum class SearchTarget {
        NONE, START_POINT, END_POINT, STOPS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSmartRoutePlannerBinding.inflate(inflater, container, false)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Setup WebView
        setupWebView()

        // Setup RecyclerView for search results
        setupRecyclerView()

        // Setup search input and destination/start click
        setupSearchAndRouteInputs()

        // Setup button click listeners
        setupButtons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load the map after view is created
        loadMap()
    }

    // Method to set the start point
    fun setStartPoint(name: String, latitude: Double, longitude: Double) {
        startName = name
        startLatitude = latitude
        startLongitude = longitude
        binding.txtStart.text = name // Update the text view to show the current start point
    }

    // Method to set the stop point
    fun setStopPoint(name: String, latitude: Double, longitude: Double) {
        stopName = name
        stopLatitude = latitude
        stopLongitude = longitude
        hasStop = true
        binding.txtStops.text = name // Update the text view to show the current stop point
        binding.txtStops.visibility = View.VISIBLE // Make stop text visible
        binding.btnRemoveStop.visibility = View.VISIBLE // Show remove stop button
    }

    // Method to remove the stop point
    fun removeStopPoint() {
        stopName = ""
        stopLatitude = 0.0
        stopLongitude = 0.0
        hasStop = false
        binding.txtStops.text = ""
        binding.btnRemoveStop.visibility = View.GONE // Hide remove stop button
    }

    // Method to set the end point (destination)
    fun setEndPoint(name: String, latitude: Double, longitude: Double) {
        destinationName = name
        destinationLatitude = latitude
        destinationLongitude = longitude
        binding.txtDestination.text = name // Update the text view to show the current destination
    }

    private fun setupButtons() {
        // Create Route button - now uses stored start, stop (if any), and end points
        binding.btnAddMarker.setOnClickListener { // Renamed from Add Marker conceptually
            mediator_createRouteToDestination()
        }

        // Swap location details
        binding.btnSwap.setOnClickListener {
            // Store current start details in temporary variables
            val tempStartName = startName
            val tempStartLatitude = startLatitude
            val tempStartLongitude = startLongitude

            // Set start details to current destination details
            setStartPoint(destinationName, destinationLatitude, destinationLongitude)

            // Set destination details to the original start details (now in temporary variables)
            setEndPoint(tempStartName, tempStartLatitude, tempStartLongitude)

            // Optionally, re-create the route with the swapped points
            mediator_createRouteToDestination()

            showToast("Start and Destination swapped!")
        }

        // Remove stop button
        binding.btnRemoveStop.setOnClickListener {
            removeStopPoint()
            showToast("Stop removed!")
            // Recreate route without stop
            mediator_createRouteToDestination()
        }

        binding.btnHideDirections.setOnClickListener {
            hideDirectionsPanel()
        }

        binding.btnShowDirections.setOnClickListener {
            showDirectionsPanel()
        }

        binding.btnResetSearch.setOnClickListener {
            binding.txtSearch.setText("")
        }

        // Button to set start point to current location
        binding.btnCurrent.setOnClickListener {
            binding.btnCurrent.visibility = View.GONE
            binding.frmSearch.visibility = View.GONE

            if (isLocationPermissionGranted()) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            setStartPoint("Current Location", location.latitude, location.longitude)
                            showToast("Start point set to Current Location.")
                        } else {
                            showToast("Current location not available. Please try again.")
                        }
                    }.addOnFailureListener {
                        showToast("Failed to get current location: ${it.message}.")
                    }
                } catch (e: SecurityException) {
                    showToast("Location permission denied. Cannot set current location.")
                }
            } else {
                showToast("Location permission not granted. Please grant permission to use current location.")
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Call this each time when you want to refresh the locations
    fun mediator_createRouteToDestination() {
        // Clear previous markers and route before adding new ones
        clearAllMarkers()
        clearAllRoutes()

        // Create route based on whether we have a stop or not
        if (hasStop) {
            createRouteWithStop(
                startLatitude, startLongitude,
                stopLatitude, stopLongitude,
                destinationLatitude, destinationLongitude,
                startName, stopName, destinationName
            )
            showToast("Route created from $startName to $destinationName via $stopName!")
        } else {
            createRouteToDestination(
                startLatitude, startLongitude,
                destinationLatitude, destinationLongitude,
                startName, destinationName
            )
            showToast("Route created from $startName to $destinationName!")
        }
    }

    private fun setupRecyclerView() {
        geocodingResultsAdapter = GeocodingResultsAdapter(geocodingResultsList) { result ->
            // Handle item click based on current search target
            when (currentSearchTarget) {
                SearchTarget.START_POINT -> {
                    setStartPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.END_POINT -> {
                    setEndPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.STOPS -> {
                    setStopPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.NONE -> {
                    // Should not happen if logic is correct
                }
            }
            binding.frmSearch.visibility = View.GONE // Hide search frame
            binding.txtSearch.setText("") // Clear search box
            binding.txtSearch.clearFocus() // Clear focus from search box
            hideKeyboard(binding.txtSearch) // Hide keyboard
            geocodingResultsAdapter.updateResults(emptyList()) // Clear results
            currentSearchTarget = SearchTarget.NONE // Reset search target
        }
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = geocodingResultsAdapter
        }
    }

    private fun setupSearchAndRouteInputs() {
        // Listener for changes in destination text (to show/hide btnAddMarker)
        binding.txtDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Show start button if destination is not empty
                if (!binding.txtDestination.text.toString().isNullOrEmpty()) {
                    binding.btnStart.visibility = View.VISIBLE
                } else {
                    binding.btnStart.visibility = View.GONE
                }
                updateRouteButtonsVisibility()
            }
        })

        // Listener for changes in start text (to show/hide buttons)
        binding.txtStart.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateRouteButtonsVisibility()
            }
        })

        // When btnStart (the UI element for Start point) is clicked, open search for start point
        binding.btnStart.setOnClickListener {
            binding.btnCurrent.visibility = View.VISIBLE
            currentSearchTarget = SearchTarget.START_POINT
            binding.frmSearch.visibility = View.VISIBLE
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            if (startName != "Your Current Location") {
                binding.txtSearch.setText(binding.txtStart.text) // Pre-fill with current start name
            } else {
                binding.txtSearch.setText("") // Pre-fill with current start name
            }
            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }

        // When btnDestination (the UI element for Destination) is clicked, open search for end point
        binding.btnDestination.setOnClickListener {
            binding.btnCurrent.visibility = View.GONE
            currentSearchTarget = SearchTarget.END_POINT
            binding.frmSearch.visibility = View.VISIBLE
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            binding.txtSearch.setText(binding.txtDestination.text) // Pre-fill with current destination name
            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }

        // When btnStops is clicked, open search for stop point
        binding.btnStops.setOnClickListener {
            binding.btnCurrent.visibility = View.GONE
            currentSearchTarget = SearchTarget.STOPS
            binding.frmSearch.visibility = View.VISIBLE
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            if (hasStop) {
                binding.txtSearch.setText(binding.txtStops.text) // Pre-fill with current stop name
            } else {
                binding.txtSearch.setText("") // Empty for new stop
            }
            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }

        // Handle back button in search frame
        binding.btnBackToMap.setOnClickListener {
            binding.frmSearch.visibility = View.GONE
            binding.txtSearch.clearFocus()
            hideKeyboard(binding.txtSearch)
            geocodingResultsAdapter.updateResults(emptyList()) // Clear results when going back
            currentSearchTarget = SearchTarget.NONE // Reset search target
        }

        binding.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.length > 2) { // Start search after 2 characters
                    searchJob?.cancel() // Cancel previous search job
                    searchJob = searchScope.launch {
                        delay(500) // Debounce search to prevent too many API calls
                        performGeocodingSearch(query)
                    }
                } else {
                    binding.rvSearchResults.visibility = View.GONE
                    geocodingResultsAdapter.updateResults(emptyList()) // Clear results if query is too short
                }
            }
        })

        // Handle search button on keyboard
        binding.txtSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.txtSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performGeocodingSearch(query)
                }
                hideKeyboard(v)
                true
            } else {
                false
            }
        }
    }

    // Helper method to update route buttons visibility
    private fun updateRouteButtonsVisibility() {
        if (!binding.txtDestination.text.toString().isNullOrEmpty() && !binding.txtStart.text.toString().isNullOrEmpty()) {
            binding.btnAddMarker.visibility = View.VISIBLE
            binding.btnSwap.visibility = View.VISIBLE
            binding.btnStops.visibility = View.VISIBLE
        } else {
            binding.btnAddMarker.visibility = View.GONE
            binding.btnSwap.visibility = View.GONE
            binding.btnStops.visibility = View.GONE
        }
    }

    private fun performGeocodingSearch(query: String) {
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=${query}&limit=10"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TourNoteAndroidApp/1.0 (contact@example.com - replace with your app info)")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    showToast("Geocoding failed: Network error - ${e.message}")
                    binding.rvSearchResults.visibility = View.GONE
                    Log.e("Geocoding", "Network failure for '$query': ${e.message}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    activity?.runOnUiThread {
                        showToast("Geocoding failed: HTTP ${response.code}. Please try again.")
                        binding.rvSearchResults.visibility = View.GONE
                        Log.e("Geocoding", "Unsuccessful HTTP response for '$query': Code ${response.code}, Body: ${errorBody?.take(500)}")
                    }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val results = mutableListOf<GeocodingResultsDataClass>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val name = jsonObject.optString("display_name")
                            val lat = jsonObject.optDouble("lat")
                            val lon = jsonObject.optDouble("lon")
                            if (name.isNotEmpty() && lat != 0.0 && lon != 0.0) {
                                results.add(GeocodingResultsDataClass(name, lat, lon))
                            }
                        }

                        activity?.runOnUiThread {
                            if (results.isNotEmpty()) {
                                geocodingResultsAdapter.updateResults(results)
                                binding.rvSearchResults.visibility = View.VISIBLE
                            } else {
                                geocodingResultsAdapter.updateResults(emptyList())
                                binding.rvSearchResults.visibility = View.GONE
                                showToast("No results found for '$query'")
                            }
                        }

                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            showToast("Error parsing geocoding response. See Logcat for details.")
                            binding.rvSearchResults.visibility = View.GONE
                            Log.e("Geocoding", "JSON parsing error for query '$query': ${e.message}. Raw Response:\n$responseBody", e)
                        }
                    }
                } ?: activity?.runOnUiThread {
                    showToast("Empty geocoding response body.")
                    binding.rvSearchResults.visibility = View.GONE
                    Log.w("Geocoding", "Empty response body for query: '$query'")
                }
            }
        })
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupWebView() {
        webView = binding.leafletWebView

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(true)
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateMapLocation(startLatitude, startLongitude) // Center map on initial start point
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                showToast("Error loading map: $description")
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")
    }

    private fun loadMap() {
        webView.loadUrl("file:///android_asset/leaflet_map.html")
    }

    private fun getCurrentLocation() {
        if (!isLocationPermissionGranted()) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        getLastKnownLocation()
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    updateMapLocation(it.latitude, it.longitude)
                    showToast("Map centered on your current location.")
                } ?: run {
                    showToast("No recent location found. Centering map on default view.")
                    updateMapLocation(28.6139, 77.2090) // New Delhi coordinates
                }
            }.addOnFailureListener { exception ->
                showToast("Failed to get location: ${exception.message}. Centering map on default view.")
                updateMapLocation(28.6139, 77.2090) // New Delhi coordinates
            }
        } catch (e: SecurityException) {
            showToast("Location permission denied. Centering map on default view.")
            updateMapLocation(28.6139, 77.2090) // New Delhi coordinates
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val script = "updateLocation($latitude, $longitude);"
        webView.evaluateJavascript(script, null)
    }

    fun addMarkerToMap(latitude: Double, longitude: Double, popup: String) {
        val script = "addMarker($latitude, $longitude, '$popup');"
        webView.evaluateJavascript(script, null)
    }

    fun centerMapOnLocation(latitude: Double, longitude: Double, zoom: Int = 13) {
        val script = "map.setView([$latitude, $longitude], $zoom);"
        webView.evaluateJavascript(script, null)
    }

    fun clearAllMarkers() {
        val script = "clearMarkers();"
        webView.evaluateJavascript(script, null)
    }

    fun clearAllRoutes() {
        val script = "clearRoutes();"
        webView.evaluateJavascript(script, null)
    }

    fun hideDirectionsPanel() {
        val script = "hideDirections();"
        webView.evaluateJavascript(script, null)
    }

    fun showDirectionsPanel() {
        val script = "showDirections();"
        webView.evaluateJavascript(script, null)
    }

    fun minimizeDirectionsPanel() {
        val script = "minimizeDirections();"
        webView.evaluateJavascript(script, null)
    }

    // Updated method to create route using stored start and end names
    fun createRouteToDestination(startLat: Double, startLng: Double, destLat: Double, destLng: Double, startName: String, destName: String) {
        val script = "createRoute($startLat, $startLng, $destLat, $destLng, '$startName', '$destName');"
        webView.evaluateJavascript(script, null)
    }

    // New method to create route with one stop
    fun createRouteWithStop(startLat: Double, startLng: Double, stopLat: Double, stopLng: Double, destLat: Double, destLng: Double, startName: String, stopName: String, destName: String) {
        val script = "createRouteWithOneStop($startLat, $startLng, $stopLat, $stopLng, $destLat, $destLng, '$startName', '$stopName', '$destName');"
        webView.evaluateJavascript(script, null)
    }

    fun addRoute(waypoints: List<Pair<Double, Double>>) {
        val waypointsJson = waypoints.joinToString(",") { "[${it.first}, ${it.second}]" }
        val script = "addRoute([$waypointsJson]);"
        webView.evaluateJavascript(script, null)
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Location permission granted")
                    if (currentSearchTarget == SearchTarget.START_POINT) {
                        getLastKnownLocation()
                    }
                } else {
                    showToast("Location permission denied - using default map view")
                    updateMapLocation(28.6139, 77.2090) // New Delhi coordinates
                }
            }
        }
    }

    inner class WebAppInterface {
        @android.webkit.JavascriptInterface
        fun showToast(message: String) {
            activity?.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }

        @android.webkit.JavascriptInterface
        fun onMarkerClick(latitude: Double, longitude: Double, title: String) {
            activity?.runOnUiThread {
                showToast("Marker clicked: $title at ($latitude, $longitude)")
            }
        }

        @android.webkit.JavascriptInterface
        fun onMapClick(latitude: Double, longitude: Double) {
            activity?.runOnUiThread {
                showToast("Map clicked at: ($latitude, $longitude)")
            }
        }

        @android.webkit.JavascriptInterface
        fun onRouteFound(distance: String, duration: String) {
            activity?.runOnUiThread {
                showToast("Route found: $distance, $duration")
            }
        }

        @android.webkit.JavascriptInterface
        fun onRouteFoundWithStop(distance: String, duration: String) {
            activity?.runOnUiThread {
                showToast("Route with stop found: $distance, $duration")
            }
        }

        @android.webkit.JavascriptInterface
        fun onRouteError(error: String) {
            activity?.runOnUiThread {
                showToast("Route error: $error")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}