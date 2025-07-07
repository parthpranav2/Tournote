package com.example.tournote.Functionality.Segments.SmartRoutePlanner.Fragment

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
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tournote.Functionality.Segments.SmartRoutePlanner.Adapter.GeocodingResultsAdapter
import com.example.tournote.Functionality.Segments.SmartRoutePlanner.DataClass.GeocodingResultsDataClass
import com.example.tournote.Functionality.Segments.SmartRoutePlanner.Adapter.RoutePointsAdapter
import com.example.tournote.RoutePointDataClass
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

class SmartRoutePlannerFragment: Fragment() {

    private var _binding: FragmentSmartRoutePlannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    // New: List to hold all route points (start, stops, end)
    private val fullRoutePoints = mutableListOf<RoutePointDataClass>()
    private lateinit var routePointsAdapter: RoutePointsAdapter

    // Search related
    private lateinit var geocodingResultsAdapter: GeocodingResultsAdapter
    private val geocodingResultsList = mutableListOf<GeocodingResultsDataClass>()
    private val searchScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null
    private val httpClient = OkHttpClient()

    private var RouteTime: Int = 0
    private var RouteDistance: Int = 0

    private var initialRouteTime: Int = 0
    private var initialRouteDistance: Int = 0

    private var isSmartRouteEnabled = false

    // Enum to keep track of which field is currently being searched/edited
    private var currentSearchTarget: SearchTarget = SearchTarget.NONE

    enum class SearchTarget {
        NONE, START_POINT, END_POINT, ADD_STOP, EDIT_STOP
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSmartRoutePlannerBinding.inflate(inflater, container, false)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize fullRoutePoints with default start and end points
        if (fullRoutePoints.isEmpty()) {
            // Try to fetch current location
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        fullRoutePoints.add(
                            RoutePointDataClass(
                                "Current Location",
                                location.latitude,
                                location.longitude,
                                isStartPoint = true
                            )
                        )
                        updateRoutePointsUI()
                    } else {
                        // Fallback: prompt user or show placeholder
                        fullRoutePoints.add(
                            RoutePointDataClass(
                                "Set Start Point",
                                0.0,
                                0.0,
                                isStartPoint = true
                            )
                        )
                    }
                }
            } else {
                // Request permission or show placeholder
                fullRoutePoints.add(
                    RoutePointDataClass(
                        "Set Start Point",
                        0.0,
                        0.0,
                        isStartPoint = true
                    )
                )
            }
            // Add a placeholder for the end point if the list is empty or only has a start point
            if (fullRoutePoints.size == 1 && fullRoutePoints.first().isStartPoint) {
                fullRoutePoints.add(
                    RoutePointDataClass(
                        "Set Destination",
                        0.0,
                        0.0,
                        isEndPoint = true
                    )
                )
            }
        }

        // Setup WebView
        setupWebView()

        // Setup RecyclerView for search results
        setupGeocodingRecyclerView()

        // Setup RecyclerView for full route points
        setupRoutePointsRecyclerView()

        // Setup search input and destination/start click
        setupSearchAndRouteInputs()

        // Setup button click listeners
        setupButtons()

        // Initial UI update
        updateRoutePointsUI()
        updateStopsCountText()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load the map after view is created
        loadMap()
    }

    // New: Helper to update UI elements based on fullRoutePoints
    private fun updateRoutePointsUI() {
        binding.txtStart.text = fullRoutePoints.firstOrNull { it.isStartPoint }?.name ?: "Set Start Point"
        binding.txtDestination.text = fullRoutePoints.firstOrNull { it.isEndPoint }?.name ?: "Set Destination"
        routePointsAdapter.updateRoutePoints(fullRoutePoints.toList())
        updateStopsCountText()
        updateRouteButtonsVisibility()
    }

    // New: Helper to update the stops count text
    private fun updateStopsCountText() {
        val stopsCount = routePointsAdapter.getStopsCount()
        if (stopsCount > 0) {
            binding.txtStops.text = "$stopsCount stops"
            binding.txtStops.visibility = View.VISIBLE
        } else {
            binding.txtStops.text = "Add stops"
            binding.txtStops.visibility = View.VISIBLE // Always visible to allow adding
        }
    }

    // Method to set the start point
    fun setStartPoint(name: String, latitude: Double, longitude: Double) {
        val currentStart = fullRoutePoints.firstOrNull { it.isStartPoint }
        if (currentStart != null) {
            val index = fullRoutePoints.indexOf(currentStart)
            fullRoutePoints[index] = RoutePointDataClass(name, latitude, longitude, isStartPoint = true)
        } else {
            // Should not happen if initialized correctly, but as a fallback
            fullRoutePoints.add(0, RoutePointDataClass(name, latitude, longitude, isStartPoint = true))
        }
        updateRoutePointsUI()
        mediator_createRouteToDestination()
    }

    // Method to add a stop point (now adds to the list)
    fun addStopPoint(name: String, latitude: Double, longitude: Double) {
        val newStop = RoutePointDataClass(name, latitude, longitude)
        routePointsAdapter.addRoutePoint(newStop) // Adapter handles insertion logic
        updateRoutePointsUI()
        mediator_createRouteToDestination()
    }

    // Method to remove a stop point (triggered by adapter callback)
    fun removeStopPoint(position: Int) {
        if (position >= 0 && position < fullRoutePoints.size) {
            val removedPoint = fullRoutePoints[position]
            if (!removedPoint.isStartPoint && !removedPoint.isEndPoint) { // Only remove actual stops
                routePointsAdapter.removeRoutePoint(position)
                updateRoutePointsUI()
                mediator_createRouteToDestination()
            }
        }
    }

    // Method to set the end point (destination)
    fun setEndPoint(name: String, latitude: Double, longitude: Double) {
        val currentEnd = fullRoutePoints.firstOrNull { it.isEndPoint }
        if (currentEnd != null) {
            val index = fullRoutePoints.indexOf(currentEnd)
            fullRoutePoints[index] = RoutePointDataClass(name, latitude, longitude, isEndPoint = true)
        } else {
            // Should not happen, but as a fallback
            fullRoutePoints.add(RoutePointDataClass(name, latitude, longitude, isEndPoint = true))
        }
        updateRoutePointsUI()
        mediator_createRouteToDestination()
    }

    private fun setupButtons() {
        // Create Route button now uses stored start, stop (if any), and end points
        binding.btnAddMarker.setOnClickListener { // Renamed from Add Marker conceptually
            isSmartRouteEnabled=false
            mediator_createRouteToDestination()
        }

        binding.btnSmartRoute.setOnClickListener {

            // Extracting initial details of set route
            val hours = Regex("""(\d+)\s*hr""").find(binding.textViewTotalTripTime.text.toString())?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val minutes = Regex("""(\d+)\s*min""").find(binding.textViewTotalTripTime.text.toString())?.groupValues?.get(1)?.toIntOrNull() ?: 0

            initialRouteTime = hours * 60 + minutes

            val distance = Regex("""(\d+)\s*km""").find(binding.textViewTotalTripDistance.text.toString())?.groupValues?.get(1)?.toIntOrNull() ?: 0
            initialRouteDistance = distance


            val startPoint = fullRoutePoints.firstOrNull { it.isStartPoint }
            val endPoint = fullRoutePoints.firstOrNull { it.isEndPoint }
            val stops = fullRoutePoints.filter { !it.isStartPoint && !it.isEndPoint }

            if (startPoint != null && endPoint != null) {
                val stopsForJs = stops.map { mapOf("lat" to it.latitude, "lng" to it.longitude, "name" to it.name) }
                val startName = startPoint.name
                val endName = endPoint.name
                val stopNames = stops.map { it.name }

                val stopsJson = stopsForJs.joinToString(",") {
                    val lat = it["lat"]
                    val lng = it["lng"]
                    val name = it["name"]?.toString()?.replace("'", "\\'") // Escape single quotes
                    "{lat:$lat, lng:$lng, name:'$name'}"
                }

                val script = "performSmartRouting(${startPoint.latitude}, ${startPoint.longitude}, [$stopsJson], ${endPoint.latitude}, ${endPoint.longitude}, '$startName', [${stopNames.joinToString(",") { "'${it.replace("'", "\\'")}'" }}], '$endName');"
                webView.evaluateJavascript(script, null)

                isSmartRouteEnabled=true
            } else {
                showToast("Please set both start and end points for smart routing.")
            }

        }

        // Swap location details
        binding.btnSwap.setOnClickListener {
            val startPoint = fullRoutePoints.firstOrNull { it.isStartPoint }
            val endPoint = fullRoutePoints.firstOrNull { it.isEndPoint }

            if (startPoint != null && endPoint != null) {
                // Temporarily store start details
                val tempStartName = startPoint.name
                val tempStartLatitude = startPoint.latitude
                val tempStartLongitude = startPoint.longitude

                // Set start details to current destination details
                setStartPoint(endPoint.name, endPoint.latitude, endPoint.longitude)

                // Set destination details to the original start details
                setEndPoint(tempStartName, tempStartLatitude, tempStartLongitude)

                showToast("Start and Destination swapped!")
            } else {
                showToast("Cannot swap: Start or End point not defined.")
            }
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
            hideKeyboard(binding.txtSearch)
            geocodingResultsAdapter.updateResults(emptyList())
            currentSearchTarget = SearchTarget.NONE

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

        val waypoints = fullRoutePoints.map { Pair(it.latitude, it.longitude) }
        val waypointNames = fullRoutePoints.map { it.name }

        if (waypoints.size >= 2) {
            createRouteWithMultipleStops(waypoints, waypointNames)
            showToast("Route created with ${routePointsAdapter.getStopsCount()} stops!")
        } else {
            showToast("Please add at least a start and end point to create a route.")
        }

    }

    private fun setupGeocodingRecyclerView() {
        geocodingResultsAdapter = GeocodingResultsAdapter(geocodingResultsList) { result ->
            // Handle item click based on current search target
            when (currentSearchTarget) {
                SearchTarget.START_POINT -> {
                    setStartPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.END_POINT -> {
                    setEndPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.ADD_STOP -> {
                    addStopPoint(result.name, result.latitude, result.longitude)
                }
                SearchTarget.EDIT_STOP -> {
                    // Implement editing a specific stop if needed
                    showToast("Editing stops is not yet implemented. Please remove and re-add.")
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

    // New: Setup RecyclerView for displaying full route points
    private fun setupRoutePointsRecyclerView() {
        routePointsAdapter = RoutePointsAdapter(
            fullRoutePoints, // Pass the mutable list directly
            onItemClick = { routePoint, position ->
                // Handle click on a route point in the full route list (e.g., center map)
                centerMapOnLocation(routePoint.latitude, routePoint.longitude)
                showToast("Centered map on ${routePoint.name}")
            },
            onRemoveClick = { position ->
                // Handle remove click from the adapter
                removeStopPoint(position)
                showToast("Stop removed!")
            }
        )
        binding.rvFullRoute.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = routePointsAdapter
        }
    }

    private fun setupSearchAndRouteInputs() {
        // Listener for changes in destination text (now managed by fullRoutePoints)
        binding.txtDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateRouteButtonsVisibility()
            }
        })
        // Listener for changes in start text (now managed by fullRoutePoints)
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
            binding.frmFullRoute.visibility = View.GONE // Hide full route list
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            val currentStartName = fullRoutePoints.firstOrNull { it.isStartPoint }?.name

            if (currentStartName != "Current Location") {
                binding.txtSearch.setText(currentStartName)
            } else {
                binding.txtSearch.setText("")
            }

            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }
        // When btnDestination (the UI element for Destination) is clicked, open search for end point
        binding.btnDestination.setOnClickListener {
            binding.btnCurrent.visibility = View.GONE
            currentSearchTarget = SearchTarget.END_POINT
            binding.frmSearch.visibility = View.VISIBLE
            binding.frmFullRoute.visibility = View.GONE // Hide full route list
            binding.txtSearch.requestFocus()
            showKeyboard(binding.txtSearch)
            val currentEndName = fullRoutePoints.firstOrNull { it.isEndPoint }?.name
            binding.txtSearch.setText(currentEndName)

            binding.txtSearch.setSelection(binding.txtSearch.text.length)
        }
        // When btnStops is clicked, show the full route recycler view
        binding.btnStops.setOnClickListener {
            binding.frmSearch.visibility = View.GONE // Hide search if open
            binding.frmFullRoute.visibility = View.VISIBLE // Show full route list
            binding.txtSearch.clearFocus()
            hideKeyboard(binding.txtSearch)

            geocodingResultsAdapter.updateResults(emptyList()) // Clear search results
            currentSearchTarget = SearchTarget.NONE // Reset search target
            // Add a click listener to the "Add Stop" button within the frmFinalRoute
            binding.btnAddStopInRoute.setOnClickListener {
                isSmartRouteEnabled=false

                currentSearchTarget = SearchTarget.ADD_STOP
                binding.frmSearch.visibility = View.VISIBLE // Show search frame to add a new stop
                binding.frmFullRoute.visibility = View.GONE // Hide full route list temporarily
                binding.txtSearch.requestFocus()
                showKeyboard(binding.txtSearch)
                binding.txtSearch.setText("") // Clear search box for new stop
            }
        }
        // Handle back button in search frame
        binding.btnBackToMap.setOnClickListener {
            binding.frmSearch.visibility = View.GONE
            binding.frmFullRoute.visibility = View.GONE // Hide full route list when going back to map
            binding.txtSearch.clearFocus()
            hideKeyboard(binding.txtSearch)

            geocodingResultsAdapter.updateResults(emptyList()) // Clear results when going back
            currentSearchTarget = SearchTarget.NONE // Reset search target
        }

        // Handle back button in full route frame
        binding.btnBackToMap1.setOnClickListener {
            binding.frmFullRoute.visibility = View.GONE // Hide full route list
            binding.frmSearch.visibility = View.GONE // Ensure search is also hidden
            binding.txtSearch.clearFocus()
            hideKeyboard(binding.txtSearch)

            geocodingResultsAdapter.updateResults(emptyList())
            currentSearchTarget = SearchTarget.NONE
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
        val hasStart = fullRoutePoints.any { it.isStartPoint && it.name.isNotEmpty() && (it.latitude != 0.0 || it.longitude != 0.0) }
        val hasEnd = fullRoutePoints.any { it.isEndPoint && it.name.isNotEmpty() && (it.latitude != 0.0 || it.longitude != 0.0) }

        if(hasEnd){
            binding.btnStart.visibility= View.VISIBLE
            binding.btnSwap.visibility = View.VISIBLE
        }else{
            binding.btnStart.visibility= View.GONE
            binding.btnSwap.visibility = View.GONE
        }

        if (hasStart && hasEnd) {
            binding.btnAddMarker.visibility = View.VISIBLE
            binding.btnSmartRoute.visibility = View.VISIBLE
            binding.btnStops.visibility = View.VISIBLE
        } else {
            binding.btnAddMarker.visibility = View.GONE
            binding.btnSmartRoute.visibility = View.GONE
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
                // Center map on initial start point
                val startPoint = fullRoutePoints.firstOrNull { it.isStartPoint }

                if (startPoint != null && (startPoint.latitude != 0.0 || startPoint.longitude != 0.0)) {
                    updateMapLocation(startPoint.latitude, startPoint.longitude)
                } else {
                    updateMapLocation(28.6139, 77.2090) // Default to New Delhi
                }
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

    // New: Function to create route with multiple stops
    fun createRouteWithMultipleStops(waypoints: List<Pair<Double, Double>>, waypointNames: List<String>) {
        val waypointsJson = waypoints.joinToString(",") { "[${it.first}, ${it.second}]" }
        val namesJson = waypointNames.joinToString(",") { "'${it.replace("'", "\\'")}'" } // Escape single quotes
        val script = "createRouteWithMultipleStops([$waypointsJson], [$namesJson]);"
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
        @JavascriptInterface
        fun showToast(message: String) {
            activity?.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun onMarkerClick(latitude: Double, longitude: Double, title: String) {
            activity?.runOnUiThread {
                showToast("Marker clicked: $title at ($latitude, $longitude)")
            }
        }

        @JavascriptInterface
        fun onMapClick(latitude: Double, longitude: Double) {
            activity?.runOnUiThread {
                showToast("Map clicked at: ($latitude, $longitude)")
            }
        }

        @JavascriptInterface
        fun onRouteFound(distance: String, duration: String) {
            activity?.runOnUiThread {
                showToast("Route found: $distance, $duration")
                // Only update if it's not a smart route calculation

                    val totalMinutes = Regex("""\d+""").find(duration)?.value?.toIntOrNull() ?: 0
                    val totalDistance = Regex("""\d+""").find(distance)?.value?.toIntOrNull() ?: 0
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    val timeString = buildString {
                        append("Total trip time: ")
                        if (hours > 0) append("$hours hr ")
                        if (minutes > 0) append("$minutes min")
                    }
                    binding.textViewTotalTripTime.text = timeString.trim()
                    binding.textViewTotalTripDistance.text = "Total trip Distance : $totalDistance km"
                    binding.routeDetails.visibility = View.VISIBLE
            }
        }

        @JavascriptInterface
        fun onRouteFoundWithStop(distance: String, duration: String) {
            activity?.runOnUiThread {
                showToast("Route with stop found: $distance, $duration")
                // Only update if it's not a smart route calculation
                    val totalMinutes = Regex("""\d+""").find(duration)?.value?.toIntOrNull() ?: 0
                    val totalDistance = Regex("""\d+""").find(distance)?.value?.toIntOrNull() ?: 0
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60
                    val timeString = buildString {
                        append("Total trip time: ")
                        if (hours > 0) append("$hours hr ")
                        if (minutes > 0) append("$minutes min")
                    }
                    binding.textViewTotalTripTime.text = timeString.trim()
                    binding.textViewTotalTripDistance.text = "Total trip Distance : $totalDistance km"
                    binding.routeDetails.visibility = View.VISIBLE
            }
        }

        @JavascriptInterface
        fun onRouteFoundWithMultipleStops(distance: String, duration: String, numWaypoints: Int) {

            activity?.runOnUiThread {
                val numStops = numWaypoints - 2 // Subtract start and end points
                // Safely extract numeric part using regex
                val totalMinutes = Regex("""\d+""").find(duration)?.value?.toIntOrNull() ?: 0
                val totalDistance = Regex("""\d+""").find(distance)?.value?.toIntOrNull() ?: 0

                RouteTime = totalMinutes
                RouteDistance = totalDistance
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                val timeString = buildString {
                    append("Total trip time: ")
                    if (hours > 0) append("$hours hr ")
                    if (minutes > 0) append("$minutes min")
                }

                if(isSmartRouteEnabled){
                    binding.labelSmartRoute.visibility=View.VISIBLE

                    if(((initialRouteTime-totalMinutes)>0)&&(initialRouteDistance-RouteDistance)>0){
                        binding.textViewFinalComment.visibility=View.VISIBLE

                        var savedHours = (initialRouteTime-totalMinutes)/ 60
                        var savedMinutes = (initialRouteTime-totalMinutes)% 60

                        val commentString = buildString {
                            append("You save ")
                            if (savedHours > 0) append("$savedHours hr ")
                            if (savedMinutes > 0) append("$savedMinutes min ")
                            append("and "+(initialRouteDistance-RouteDistance)+" km with this route.")
                        }

                        binding.textViewFinalComment.text = commentString.trim()
                    }else{
                        binding.textViewFinalComment.visibility=View.GONE
                    }

                }else{
                    binding.labelSmartRoute.visibility=View.GONE
                    binding.textViewFinalComment.visibility=View.GONE
                }

                binding.textViewTotalTripTime.text = timeString.trim()
                binding.textViewTotalTripDistance.text = "Total trip Distance : $RouteDistance km"
                binding.routeDetails.visibility = View.VISIBLE

                showToast("Route found with $numStops stops: $distance, $duration")
            }
        }

        @JavascriptInterface
        fun onRouteError(error: String) {
            activity?.runOnUiThread {
                showToast("Route error: $error")
            }
        }

        @JavascriptInterface
        fun onSmartRouteCalculated(
            minDistance: String,
            minDuration: String,
            maxDistance: String,
            maxDuration: String,
            numWaypoints: Int,
            optimalWaypointOrderJson: String // New parameter for optimal order
        ) {
            activity?.runOnUiThread {
                Log.d("SmartRoute", "Received optimalWaypointOrderJson: $optimalWaypointOrderJson") // ADD THIS LINE

                // Optionally, display max values if you have separate TextViews for them
                // For now, we'll just log them or include them in a more detailed string if needed
                Log.d("SmartRoute", "Max Time: ${maxDuration} min, Max Distance: ${maxDistance} km")

                // Parse the optimal waypoint order and update the RecyclerView
                try {
                    val jsonArray = JSONArray(optimalWaypointOrderJson)
                    val optimalRoutePoints = mutableListOf<RoutePointDataClass>()

                    Log.d("SmartRoute", "Parsed JSON array size: ${jsonArray.length()}") // ADD THIS LINE


                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val lat = jsonObject.getDouble("lat")
                        val lng = jsonObject.getDouble("lng")
                        val name = jsonObject.getString("name")
                        val isStart = jsonObject.optBoolean("isStartPoint", false)
                        val isEnd = jsonObject.optBoolean("isEndPoint", false)
                        optimalRoutePoints.add(RoutePointDataClass(name, lat, lng, isStart, isEnd))
                    }

                    Log.d("SmartRoute", "Optimal Route Points list size after parsing: ${optimalRoutePoints.size}") // ADD THIS LINE
                    if (optimalRoutePoints.isNotEmpty()) {
                        Log.d("SmartRoute", "First optimal point: ${optimalRoutePoints.first()}") // ADD THIS LINE for inspection
                        if (optimalRoutePoints.size > 1) {
                            Log.d("SmartRoute", "Last optimal point: ${optimalRoutePoints.last()}") // ADD THIS LINE for inspection
                        }
                    }

                    // Update fullRoutePoints and then the adapter
                    fullRoutePoints.clear()
                    fullRoutePoints.addAll(optimalRoutePoints)
                    routePointsAdapter.updateRoutePoints(fullRoutePoints.toList())
                    updateStopsCountText() // Update stop count based on new order

                    showToast("Optimal route calculated and displayed!")
                } catch (e: Exception) {
                    showToast("Error parsing optimal route data: ${e.message}")
                    Log.e("SmartRoute", "Error parsing optimal route data: ${e.message}", e)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}