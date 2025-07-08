package com.example.tournote.Functionality.Segments.TrackFriends

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.ValueCallback // Import ValueCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.tournote.R // Ensure this import is correct for your project
import android.util.Log // Import Log for debugging
import android.webkit.WebChromeClient
import com.example.tournote.GlobalClass

class TrackFriendsFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_track_friends, container, false)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize WebView
        webView = view.findViewById(R.id.WebView)
        setupWebView()

        // Request location permission and get location
        requestLocationPermission()

        return view
    }

    private fun setupWebView() {
        // Enable JavaScript
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        // Important for image loading from remote URLs
        webSettings.domStorageEnabled = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // Or LOAD_CACHE_ELSE_NETWORK

        // Set WebViewClient to handle page loading
        webView.webViewClient = WebViewClient()

        // Set WebChromeClient to handle console messages (for debugging)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.apply {
                    Log.d("WebViewConsole", "${message()} -- From ${sourceId()}:${lineNumber()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }
        }

        // Load the HTML file from assets folder
        webView.loadUrl("file:///android_asset/track_friends_map.html")
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionRequestCode
            )
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, do nothing or show a message
            Log.w("TrackFriendsFragment", "Location permission not granted. Cannot get current location.")
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude

                // --- IMPORTANT: Replace with the actual URL of the user's profile photo ---
                // For demonstration, using a placeholder image.
                // In a real app, this URL would come from your user data/session.
                val userProfilePhotoUrl = GlobalClass.Me?.profilePic
                // If you have a specific URL for the user's profile, use it here.
                // For instance, if you have a User object with a profilePhotoUrl property:
                // val userProfilePhotoUrl = currentUser.profilePhotoUrl ?: "default_url.png"


                // Pass location and profile photo URL to WebView
                val javascript = """
                    setUserLocation($latitude, $longitude, '$userProfilePhotoUrl');
                """.trimIndent()

                webView.post {
                    // Corrected: Explicitly create ValueCallback
                    webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                        override fun onReceiveValue(result: String?) {
                            Log.d("WebViewJS", "setUserLocation result: $result")
                        }
                    })
                }
            } ?: run {
                Log.e("TrackFriendsFragment", "Failed to get current location: Location is null")
            }
        }.addOnFailureListener { exception ->
            Log.e("TrackFriendsFragment", "Failed to get current location", exception)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get current location
                getCurrentLocation()
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
                Log.w("TrackFriendsFragment", "Location permission denied.")
                // Optionally, inform the user that map features requiring location won't work
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    // You can now expose these functions to be called by your Android code
    // For example, when you fetch friend data from a backend
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        val javascript = """
            addFriendMarker($id, '$name', $lat, $lng, '$status', '$profilePicUrl');
        """.trimIndent()
        webView.post {
            webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                override fun onReceiveValue(result: String?) {
                    Log.d("WebViewJS", "addFriendMarker result: $result")
                }
            })
        }
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        val javascript = """
            updateFriendLocation($id, $lat, $lng, '$status', '$profilePicUrl');
        """.trimIndent()
        webView.post {
            webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                override fun onReceiveValue(result: String?) {
                    Log.d("WebViewJS", "updateFriendLocation result: $result")
                }
            })
        }
    }

    fun removeFriendFromMap(id: Int) {
        val javascript = """
            removeFriendMarker($id);
        """.trimIndent()
        webView.post {
            webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                override fun onReceiveValue(result: String?) {
                    Log.d("WebViewJS", "removeFriendMarker result: $result")
                }
            })
        }
    }
}