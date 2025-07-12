package com.example.tournote.Functionality.Segments.TrackFriends

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tournote.Functionality.Repository.MainActivityRepository
import com.example.tournote.GlobalClass
import com.example.tournote.Onboarding.Repository.authRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class TrackFriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val mainRepo = MainActivityRepository()
    private val authRepo = authRepository()

    // LiveData for UI states
    private val _showPermissionRequest = MutableLiveData<Boolean>()
    val showPermissionRequest: LiveData<Boolean> = _showPermissionRequest

    private val _showMapView = MutableLiveData<Boolean>()
    val showMapView: LiveData<Boolean> = _showMapView

    private val _isWebViewReady = MutableLiveData<Boolean>()
    val isWebViewReady: LiveData<Boolean> = _isWebViewReady

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _locationUpdatePending = MutableLiveData<Boolean>()
    val locationUpdatePending: LiveData<Boolean> = _locationUpdatePending

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _trackingEnabled = MutableLiveData<Boolean>()
    val trackingEnabled: LiveData<Boolean> = _trackingEnabled

    // Friend tracking data
    private val _friendsOnMap = MutableLiveData<List<FriendMapData>>()
    val friendsOnMap: LiveData<List<FriendMapData>> = _friendsOnMap

    // WebView interaction commands
    private val _webViewCommand = MutableLiveData<WebViewCommand?>()
    val webViewCommand: LiveData<WebViewCommand?> = _webViewCommand

    // Location retry management
    private var currentLocationRetryCount = 0
    private val maxLocationRetries = 3

    init {
        _isWebViewReady.value = false
        _showPermissionRequest.value = false
        _showMapView.value = false
        _trackingEnabled.value = false
        handleUserTrackingPermissions()
    }

    private fun handleUserTrackingPermissions() {
        val currentUser = GlobalClass.Me

        if (currentUser?.email != null &&
            GlobalClass.GroupDetails_Everything.trackFriends.any { it.email == currentUser.email } == true) {
            _trackingEnabled.value = true
            showMapAndRequestLocation()
        } else {
            viewModelScope.launch {
                val isTracked = (currentUser?.email != null &&
                        GlobalClass.GroupDetails_Everything.trackFriends.any { it.email == currentUser.email } == true)

                if (GlobalClass.Me?.uid == GlobalClass.GroupDetails_Everything.owner.uid && !isTracked) {
                    enableTrackingForCurrentUser()
                    showMapAndRequestLocation()
                } else if (isTracked) {
                    _trackingEnabled.value = true
                    showMapAndRequestLocation()
                } else {
                    _showPermissionRequest.value = true
                    _showMapView.value = false
                }
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            enableTrackingForCurrentUser()
            showMapAndRequestLocation()
        }
    }

    private suspend fun enableTrackingForCurrentUser() {
        try {
            mainRepo.EnableMyTrackingOnCurrentGroup()
            val currentUserEmail = GlobalClass.Me?.email
            if (currentUserEmail != null) {
                val currentTrackFriends = GlobalClass.GroupDetails_Everything.trackFriends.toMutableList()
                if (GlobalClass.Me != null && !currentTrackFriends.any { it.email == GlobalClass.Me!!.email }) {
                    currentTrackFriends.add(GlobalClass.Me!!) // Add the current user's UserModel object
                    GlobalClass.GroupDetails_Everything = GlobalClass.GroupDetails_Everything.copy(
                        trackFriends = currentTrackFriends
                    )
                }
            }
            _trackingEnabled.value = true
        } catch (e: Exception) {
            Log.e("TrackFriendsViewModel", "Error enabling tracking", e)
            _errorMessage.value = "Error enabling tracking: ${e.message}"
        }
    }

    private fun showMapAndRequestLocation() {
        _showPermissionRequest.value = false
        _showMapView.value = true
    }

    fun onWebViewPageFinished() {
        Log.d("TrackFriendsViewModel", "WebView page finished loading")
        _isWebViewReady.value = true

        // If there's a pending location update, trigger it
        if (_locationUpdatePending.value == true) {
            _currentLocation.value?.let { location ->
                updateLocationOnMap(location)
            }
        }
    }

    fun onWebViewError(description: String?) {
        Log.e("TrackFriendsViewModel", "WebView error: $description")
        _errorMessage.value = "WebView error: $description"
    }

    fun onLocationPermissionGranted() {
        Log.d("TrackFriendsViewModel", "Location permission granted")
        // This will be handled by the fragment when it observes location permission changes
    }

    fun onLocationPermissionDenied() {
        Log.w("TrackFriendsViewModel", "Location permission denied")
        _showPermissionRequest.value = true
        _showMapView.value = false
    }

    fun updateCurrentLocation(location: Location) {
        Log.d("TrackFriendsViewModel", "Location update: ${location.latitude}, ${location.longitude}")
        _currentLocation.value = location
        currentLocationRetryCount = 0 // Reset retry count on success

        if (_isWebViewReady.value == true) {
            updateLocationOnMap(location)
        } else {
            _locationUpdatePending.value = true
        }
    }

    fun onLocationUpdateFailed(exception: Exception) {
        currentLocationRetryCount++
        Log.e("TrackFriendsViewModel", "Failed to get current location (attempt $currentLocationRetryCount)", exception)

        if (currentLocationRetryCount >= maxLocationRetries) {
            Log.e("TrackFriendsViewModel", "Max location retries reached")
            _errorMessage.value = "Unable to get current location after $maxLocationRetries attempts"
        }
    }

    fun shouldRetryLocation(): Boolean {
        return currentLocationRetryCount < maxLocationRetries
    }

    fun getCurrentRetryCount(): Int = currentLocationRetryCount

    fun isLocationRecent(location: Location): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < 3000 // 30 seconds
    }

    private fun updateLocationOnMap(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val userProfilePhotoUrl = GlobalClass.Me?.profilePic ?: ""

        if (_isWebViewReady.value == true) {
            _webViewCommand.value = WebViewCommand.UpdateUserLocation(
                latitude, longitude, userProfilePhotoUrl
            )
            _locationUpdatePending.value = false
        } else {
            _locationUpdatePending.value = true
        }
    }

    // Map interaction functions
    fun addFriendOnMap(id: Int, name: String, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for addFriendOnMap")
            return
        }

        _webViewCommand.value = WebViewCommand.AddFriendMarker(
            id, name, lat, lng, status, profilePicUrl
        )
    }

    fun updateFriendOnMap(id: Int, lat: Double, lng: Double, status: String, profilePicUrl: String) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for updateFriendOnMap")
            return
        }

        _webViewCommand.value = WebViewCommand.UpdateFriendLocation(
            id, lat, lng, status, profilePicUrl
        )
    }

    fun removeFriendFromMap(id: Int) {
        if (_isWebViewReady.value != true) {
            Log.w("TrackFriendsViewModel", "WebView not ready for removeFriendFromMap")
            return
        }

        _webViewCommand.value = WebViewCommand.RemoveFriendMarker(id)
    }

    fun clearWebViewCommand() {
        _webViewCommand.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Data classes for structured data
    data class FriendMapData(
        val id: Int,
        val name: String,
        val lat: Double,
        val lng: Double,
        val status: String,
        val profilePicUrl: String
    )

    // Sealed class for WebView commands
    sealed class WebViewCommand {
        data class UpdateUserLocation(
            val latitude: Double,
            val longitude: Double,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class AddFriendMarker(
            val id: Int,
            val name: String,
            val lat: Double,
            val lng: Double,
            val status: String,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class UpdateFriendLocation(
            val id: Int,
            val lat: Double,
            val lng: Double,
            val status: String,
            val profilePicUrl: String
        ) : WebViewCommand()

        data class RemoveFriendMarker(val id: Int) : WebViewCommand()
    }
}