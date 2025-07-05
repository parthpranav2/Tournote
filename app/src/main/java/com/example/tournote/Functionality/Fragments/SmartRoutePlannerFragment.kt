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
import com.example.tournote.Functionality.GeocodingResult
import com.example.tournote.Functionality.GeocodingResultsAdapter
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

    // Destination properties
    var destinationName: String = "Taloda" // Default to Taloda
    var destinationLatitude: Double = 21.5628
    var destinationLongitude: Double = 74.2135

    // Search related
    private lateinit var geocodingResultsAdapter: GeocodingResultsAdapter
    private val geocodingResultsList = mutableListOf<GeocodingResult>()
    private val searchScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null
    private val httpClient = OkHttpClient()

    // Enum to keep track of which field is currently being searched/edited
    private var currentSearchTarget: SearchTarget = SearchTarget.NONE

    enum class SearchTarget {
        NONE, START_POINT, END_POINT
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

        // Initialize UI with default values
        binding.txtStart.text = startName
        //binding.txtDestination.text = destinationName

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

    // Method to set the end point (destination)
    fun setEndPoint(name: String, latitude: Double, longitude: Double) {
        destinationName = name
        destinationLatitude = latitude
        destinationLongitude = longitude
        binding.txtDestination.text = name // Update the text view to show the current destination
    }

    private fun setupButtons() {
        // Create Route button - now uses stored start and end points
        binding.btnAddMarker.setOnClickListener { // Renamed from Add Marker conceptually
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
            binding.btnCurrent.visibility=View.GONE
            binding.frmSearch.visibility=View.GONE

            if (isLocationPermissionGranted()) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            setStartPoint("Current Location", location.latitude, location.longitude)
                            showToast("Start point set to Current Location.")
                        } else {
                            showToast("Current location not available. Please try again.")
                            // Optionally, fall back to a default if truly no location is found
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


    //call this each time when you want to refresh the locations
    fun mediator_createRouteToDestination(){
        // Clear previous markers and route before adding new ones
        clearAllMarkers()
        clearAllRoutes()

        // Create route from current location to destination
        createRouteToDestination(startLatitude, startLongitude, destinationLatitude, destinationLongitude, startName, destinationName)
        showToast("Route created from $startName to $destinationName!")
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
        binding.txtDestination.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Now also check txtStart for enabling route button
                if(!binding.txtDestination.text.toString().isNullOrEmpty()){
                    binding.btnStart.visibility = View.VISIBLE
                } else {
                    binding.btnStart.visibility = View.GONE
                }
            }
        })

        // Listener for changes in start text (to show/hide btnAddMarker)
        binding.txtStart.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(!binding.txtDestination.text.toString().isNullOrEmpty() && !binding.txtStart.text.toString().isNullOrEmpty()){
                    binding.btnAddMarker.visibility = View.VISIBLE
                } else {
                    binding.btnAddMarker.visibility = View.GONE
                }
            }
        })


        // When btnStart (the UI element for Start point) is clicked, open search for start point
        binding.btnStart.setOnClickListener {
            binding.btnCurrent.visibility= View.VISIBLE

            currentSearchTarget = SearchTarget.START_POINT
            binding.frmSearch.visibility = View.VISIBLE
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            if(startName!="Your Current Location"){
                binding.txtSearch.setText(binding.txtStart.text) // Pre-fill with current start name
            }else{
                binding.txtSearch.setText("") // Pre-fill with current start name
            }
            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }

        // When btnDestination (the UI element for Destination) is clicked, open search for end point
        binding.btnDestination.setOnClickListener {
            binding.btnCurrent.visibility=View.GONE

            currentSearchTarget = SearchTarget.END_POINT
            binding.frmSearch.visibility = View.VISIBLE
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            binding.txtSearch.setText(binding.txtDestination.text) // Pre-fill with current destination name
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
                        val results = mutableListOf<GeocodingResult>()
                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val name = jsonObject.optString("display_name")
                            val lat = jsonObject.optDouble("lat")
                            val lon = jsonObject.optDouble("lon")
                            if (name.isNotEmpty() && lat != 0.0 && lon != 0.0) {
                                results.add(GeocodingResult(name, lat, lon))
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
                // Optionally, get current location and update map after page loads
                // getCurrentLocation(); // Removed to avoid automatic location setting on map load for route planning
                // Instead, the map will load centered on default coordinates
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

    // This method is now primarily for initial map centering or for btnCurrent action
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
                    // This updates the map's center, not necessarily the start point for routing
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
                    // If the permission was requested because btnCurrent was pressed,
                    // try to get the location again to set it as start point.
                    if (currentSearchTarget == SearchTarget.START_POINT) { // This check might be too strict, consider if user opens search and then presses current
                        getLastKnownLocation() // Only centers map
                        // You might want to call a specific function here that re-attempts setting current location as start point
                        // after permission is granted. For simplicity, I'm relying on the button click again.
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