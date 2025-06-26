package com.example.tournote

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ApplicationClass : Application() {

    private var cloud_name: String? = null
    private var api_key: String? = null
    private var api_secret: String? = null

    // Hold a reference to the secondary FirebaseApp instance
    private var secondaryFirebaseApp: FirebaseApp? = null

    override fun onCreate() {
        super.onCreate()

        // IMPORTANT: Perform asynchronous initialization using coroutines
        GlobalScope.launch(Dispatchers.Main) { // Use Dispatchers.Main if you need UI updates, otherwise Dispatchers.IO
            try {
                // Step 1: Fetch secondary Firebase App details from primary database
                val secondaryAppDetails = fetchSecondaryAppDetailsFromPrimaryDb()

                if (secondaryAppDetails != null) {
                    // Step 2: Initialize the secondary FirebaseApp using fetched details
                    initSecondaryFirebaseApp(secondaryAppDetails)

                    // Step 3: Fetch Cloudinary config from the newly initialized secondary database
                    val configFetched = configFetcher() // Make configFetcher suspendable or use callbacks inside

                    if (configFetched) {
                        Log.d("Config", "Cloudinary config fetched successfully")

                        val config = HashMap<String, String>().apply {
                            put("cloud_name", cloud_name ?: "")
                            put("api_key", api_key ?: "")
                            put("api_secret", api_secret ?: "")
                        }

                        // Initialize MediaManager only after all configs are fetched
                        MediaManager.init(this@ApplicationClass, config)
                        Log.d("Cloudinary", "MediaManager initialized successfully")
                    } else {
                        Log.e("Config", "Failed to fetch Cloudinary config from secondary DB.")
                    }
                } else {
                    Log.e("Firebase", "Failed to fetch secondary Firebase App details from primary DB.")
                }
            } catch (e: Exception) {
                Log.e("AppInit", "Error during application initialization: ${e.message}", e)
            }
        }
    }

    /**
     * Data class to hold the fetched Firebase App details.
     * Ensure variable names match keys in your Realtime Database: 'apiKey', 'applicationId', 'databaseUrl'.
     */
    data class SecondaryFirebaseAppDetails(
        val apiKey: String = "",
        val applicationId: String = "",
        val databaseUrl: String = ""
    )

    //first layer
    private suspend fun fetchSecondaryAppDetailsFromPrimaryDb(): SecondaryFirebaseAppDetails? {
        return withContext(Dispatchers.IO) {
            try {
                val primaryDb = FirebaseDatabase.getInstance() // Accesses the DEFAULT Firebase App
                val ref = primaryDb.getReference("AppDetails/SecondaryDatabase")

                val snapshot = ref.get()
                    .await() // .await() is a suspend function from kotlinx-coroutines-play-services

                if (snapshot.exists()) {
                    val details = snapshot.getValue(SecondaryFirebaseAppDetails::class.java)
                    Log.d("FirebaseConfig", "Fetched secondary app details: $details")
                    details
                } else {
                    Log.e(
                        "FirebaseConfig",
                        "AppDetails/SecondaryDatabase path does not exist in primary DB."
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e(
                    "FirebaseConfig",
                    "Error fetching secondary app details from primary DB: ${e.message}",
                    e
                )
                null
            }
        }
    }

    //second layer
    private fun initSecondaryFirebaseApp(details: SecondaryFirebaseAppDetails) {
        val appName = "google-services-secondary" // Keep the consistent name
        val existing = FirebaseApp.getApps(this).find { it.name == appName }
        if (existing != null) {
            secondaryFirebaseApp = existing
            Log.d("FirebaseApp", "$appName already initialized.")
            return
        }

        val options = FirebaseOptions.Builder()
            .setApiKey(details.apiKey)
            .setApplicationId(details.applicationId)
            .setDatabaseUrl(details.databaseUrl)
            .build()

        secondaryFirebaseApp = FirebaseApp.initializeApp(this, options, appName)
        Log.d("FirebaseApp", "$appName initialized with fetched options.")
    }


    // Make this function a suspend function to use .await() or handle its callback internally
    private suspend fun configFetcher(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure secondaryFirebaseApp is initialized before trying to get its instance
                val app = secondaryFirebaseApp
                    ?: throw IllegalStateException("Secondary Firebase App not initialized.")
                val secondaryDb =
                    FirebaseDatabase.getInstance(app) // Use the initialized secondary app
                val ref =
                    secondaryDb.getReference("tournote") // The path to your Cloudinary config in the secondary DB

                val snapshot = ref.get().await()

                if (snapshot.exists()) {
                    val cloudName = snapshot.child("cloudname").getValue(String::class.java)
                    val apiKey = snapshot.child("apikey").getValue(String::class.java)
                    val apiSecret = snapshot.child("apisecret").getValue(String::class.java)

                    if (cloudName != null && apiKey != null && apiSecret != null) {
                        this@ApplicationClass.cloud_name = cloudName
                        this@ApplicationClass.api_key = apiKey
                        this@ApplicationClass.api_secret = apiSecret

                        true
                    } else {
                        Log.e(
                            "Firebase",
                            "Missing Cloudinary config fields in secondary DB (cloudname, apikey, apisecret)."
                        )
                        false
                    }
                } else {
                    Log.e(
                        "Firebase",
                        "'tournote' path does not exist in secondary DB for Cloudinary config."
                    )
                    false
                }
            } catch (e: Exception) {
                Log.e(
                    "Firebase",
                    "Exception during Cloudinary config fetch from secondary DB: ${e.message}",
                    e
                )
                false
            }
        }
    }
}