package com.child.app.parent

import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class ParentMapActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private var googleMap: GoogleMap? = null
    private var childLatLng: LatLng? = null
    private val TAG = "ParentMapActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_map)

        val childUid = intent.getStringExtra("childUid") ?: run {
            Log.e(TAG, "No childUid provided in intent")
            finish()
            return
        }

        setupToolbar()
        fetchChildInfo(childUid)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            Log.d(TAG, "Map is ready")
            googleMap = map
            googleMap?.uiSettings?.apply {
                isMyLocationButtonEnabled = false
                isZoomControlsEnabled = true
            }
            try {
                googleMap?.isMyLocationEnabled = false
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: ${e.message}")
            }
            
            childLatLng?.let { updateMap(it) }
            listenToChildLocation(childUid)
        }
        
        findViewById<View>(R.id.btnRefresh).setOnClickListener {
            childLatLng?.let { 
                updateMap(it)
                Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "No location data yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun fetchChildInfo(childUid: String) {
        firestore.collection("pairings")
            .whereEqualTo("childId", childUid)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                val name = doc?.getString("childName") ?: "Child"
                findViewById<TextView>(R.id.txtChildName).text = name
                findViewById<TextView>(R.id.txtAvatarLetter).text = name.take(1).uppercase()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching child info", e)
            }
    }

    private fun listenToChildLocation(childUid: String) {
        Log.d(TAG, "Listening to location for: $childUid")
        firestore.collection("childLocations")
            .document(childUid)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (doc == null || !doc.exists()) {
                    Log.w(TAG, "No location document found for $childUid")
                    return@addSnapshotListener
                }

                val lat = doc.getDouble("lat") ?: 0.0
                val lng = doc.getDouble("lng") ?: 0.0
                val timestamp = doc.getLong("timestamp") ?: 0L
                
                Log.d(TAG, "Received location update: $lat, $lng")

                if (lat == 0.0 && lng == 0.0) return@addSnapshotListener

                val pos = LatLng(lat, lng)
                childLatLng = pos
                updateMap(pos)
                
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                findViewById<TextView>(R.id.txtLastSeen).text = "Last seen: $timeAgo"
            }
    }

    private fun updateMap(pos: LatLng) {
        if (googleMap == null) {
            Log.w(TAG, "updateMap called but googleMap is null")
            return
        }
        googleMap?.apply {
            clear()
            addMarker(MarkerOptions().position(pos).title("Child Location"))
            animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
        }
    }
}
