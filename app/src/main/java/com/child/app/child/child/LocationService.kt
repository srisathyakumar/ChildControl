package com.child.app.child

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000
            ).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private val locationCallback =
        object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location: Location =
                    result.lastLocation ?: return

                val prefs =
                    getSharedPreferences("app_prefs", MODE_PRIVATE)

                val childUid =
                    prefs.getString("childUid", null) ?: return

                val data = hashMapOf(
                    "lat" to location.latitude,
                    "lng" to location.longitude,
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection("locations")
                    .document(childUid)
                    .set(data)

                GeoFenceChecker(this@LocationService)
                    .checkFence(location.latitude, location.longitude)
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null
}