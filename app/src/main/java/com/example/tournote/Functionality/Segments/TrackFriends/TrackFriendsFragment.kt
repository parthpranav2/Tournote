package com.example.tournote.Functionality.Segments.TrackFriends

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.ValueCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.tournote.R
import android.util.Log
import android.webkit.WebChromeClient
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.Repository.authRepository
import com.example.tournote.databinding.FragmentTrackFriendsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class TrackFriendsFragment : Fragment() {

    private val mainRepo = MainActivityRepository()
    private val authRepo = authRepository()

    private lateinit var binding: FragmentTrackFriendsBinding
    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackFriendsBinding.inflate(inflater, container, false)
        webView = binding.WebView // Initialize WebView here after binding is inflated
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity()) // Initialize location client here

        setupWebView() // Setup WebView early

        
        val currentUser = GlobalClass.Me

        if (currentUser?.email != null &&
            GlobalClass.GroupDetails_Everything.trackFriends?.contains(currentUser.email) == true) {
            binding.relPermissions.visibility=View.GONE
            binding.relWebView.visibility=View.VISIBLE
            showMapAndRequestLocation()
        }else{
            lifecycleScope.launch {
                val isTracked = (currentUser?.email != null &&
                        GlobalClass.GroupDetails_Everything.trackFriends?.contains(currentUser.email) == true)

                if (GlobalClass.Me?.uid == GlobalClass.GroupDetails_Everything.owner.uid && !isTracked) {
                    mainRepo.EnableMyTrackingOnCurrentGroup()
                    val currentUserEmail = currentUser?.email
                    if (currentUserEmail != null) {
                        val currentTrackFriends = GlobalClass.GroupDetails_Everything.trackFriends?.toMutableList() ?: mutableListOf()
                        if (!currentTrackFriends.contains(currentUserEmail)) {
                            currentTrackFriends.add(currentUserEmail)
                            GlobalClass.GroupDetails_Everything = GlobalClass.GroupDetails_Everything.copy(
                                trackFriends = currentTrackFriends
                            )
                        }
                    }
                    showMapAndRequestLocation()
                } else if (isTracked) {
                    showMapAndRequestLocation()
                } else {
                    binding.relPermissions.visibility = View.VISIBLE
                    binding.relWebView.visibility = View.GONE

                    binding.btnPermission.setOnClickListener {
                        lifecycleScope.launch {
                            mainRepo.EnableMyTrackingOnCurrentGroup()
                            val currentUserEmail = currentUser?.email
                            if (currentUserEmail != null) {
                                val currentTrackFriends = GlobalClass.GroupDetails_Everything.trackFriends?.toMutableList() ?: mutableListOf()
                                if (!currentTrackFriends.contains(currentUserEmail)) {
                                    currentTrackFriends.add(currentUserEmail)
                                    GlobalClass.GroupDetails_Everything = GlobalClass.GroupDetails_Everything.copy(
                                        trackFriends = currentTrackFriends
                                    )
                                }
                            }
                            showMapAndRequestLocation()
                        }
                    }
                }
            }
        }

        return binding.root
    }

    /**
     * Handles displaying the map and initiating location permission request.
     */
    private fun showMapAndRequestLocation() {
        binding.relPermissions.visibility = View.GONE
        binding.relWebView.visibility = View.VISIBLE
        requestLocationPermission()
    }

    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = WebViewClient()
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
                val userProfilePhotoUrl = GlobalClass.Me?.profilePic

                val javascript = """
                    setUserLocation($latitude, $longitude, '${userProfilePhotoUrl ?: ""}');
                """.trimIndent() // Use empty string if profilePic is null

                webView.evaluateJavascript(javascript, object : ValueCallback<String> {
                    override fun onReceiveValue(result: String?) {
                        Log.d("WebViewJS", "setUserLocation result: $result")
                    }
                })
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
                getCurrentLocation()
            } else {
                Log.w("TrackFriendsFragment", "Location permission denied.")
                // Potentially, keep showing relPermissions or inform user that map won't work
                binding.relPermissions.visibility = View.VISIBLE
                binding.relWebView.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    // region Map Interaction Functions (Exposed for Android calls)
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        val javascript = """
            addFriendMarker($id, '$name', $lat, $lng, '$status', '$profilePicUrl');
        """.trimIndent()
        webView.evaluateJavascript(javascript, object : ValueCallback<String> {
            override fun onReceiveValue(result: String?) {
                Log.d("WebViewJS", "addFriendMarker result: $result")
            }
        })
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        val javascript = """
            updateFriendLocation($id, $lat, $lng, '$status', '$profilePicUrl');
        """.trimIndent()
        webView.evaluateJavascript(javascript, object : ValueCallback<String> {
            override fun onReceiveValue(result: String?) {
                Log.d("WebViewJS", "updateFriendLocation result: $result")
            }
        })
    }

    fun removeFriendFromMap(id: Int) {
        val javascript = """
            removeFriendMarker($id);
        """.trimIndent()
        webView.evaluateJavascript(javascript, object : ValueCallback<String> {
            override fun onReceiveValue(result: String?) {
                Log.d("WebViewJS", "removeFriendMarker result: $result")
            }
        })
    }
    // endregion

    // under progress - keeping as is
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