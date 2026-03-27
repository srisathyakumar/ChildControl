package com.child.app.child

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.child.app.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geoFenceChecker: GeoFenceChecker
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val CHANNEL_ID = "location_service_channel"
    private val TAG = "LocationService"

    override fun onCreate() {
        super.onCreate()
        geoFenceChecker = GeoFenceChecker(this)
        createNotificationChannel()
        Log.d(TAG, "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(1, notification, type)
        } else {
            startForeground(1, notification)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000 // 10 seconds
        ).setMinUpdateIntervalMillis(5000)
         .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get last location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { updateFirestore(it) }
            }

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                null // Uses background thread
            )
            Log.d(TAG, "Location updates requested")
        } else {
            Log.e(TAG, "Permissions not granted for service")
            stopSelf()
        }

        return START_STICKY
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            Log.d(TAG, "onLocationResult: ${result.locations.size} locations")
            for (location in result.locations) {
                updateFirestore(location)
            }
        }
    }

    private fun updateFirestore(location: android.location.Location) {
        val childId = auth.currentUser?.uid
        if (childId == null) {
            Log.e(TAG, "No user logged in, cannot update location")
            return
        }

        val data = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("childLocations")
            .document(childId)
            .set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Location updated in Firestore: ${location.latitude}, ${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update location in Firestore", e)
            }
        
        geoFenceChecker.checkFence(location.latitude, location.longitude)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Child Control")
            .setContentText("Location tracking is active")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed")
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
