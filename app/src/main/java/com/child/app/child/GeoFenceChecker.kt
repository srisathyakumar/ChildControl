package com.child.app.child

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GeoFenceChecker(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    fun checkFence(lat: Double, lng: Double) {

        val prefs =
            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val childUid = prefs.getString("childUid", null) ?: return

        firestore.collection("geofences")
            .document(childUid)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) return@addOnSuccessListener

                val fenceLat = doc.getDouble("latitude") ?: return@addOnSuccessListener
                val fenceLng = doc.getDouble("longitude") ?: return@addOnSuccessListener
                val radius = doc.getLong("radius") ?: return@addOnSuccessListener

                val distance = distanceMeters(lat, lng, fenceLat, fenceLng)

                if (distance > radius) {

                    firestore.collection("alerts")
                        .document(childUid)
                        .set(
                            mapOf(
                                "alert" to "geofence_violation",
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                }
            }
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) *
                    sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}