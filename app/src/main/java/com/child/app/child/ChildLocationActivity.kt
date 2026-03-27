package com.child.app.child

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.child.app.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ChildLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val childId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var lastLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.location)

        handleWindowInsets()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnRefresh).setOnClickListener {
            handleRefreshClick()
        }
        
        loadAvatarLetter()
    }

    private fun handleWindowInsets() {
        val mainView = findViewById<View>(R.id.main)
        val headerLayout = findViewById<View>(R.id.headerLayout)
        val bottomCard = findViewById<View>(R.id.bottomCard)

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Match the padding/positioning of the stats page profile pill
            headerLayout.updatePadding(top = systemBars.top + 16.dpToPx())

            // Position bottom card with margin from bottom including navigation bar
            bottomCard.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 23.dpToPx()
            }

            insets
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun handleRefreshClick() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            showEnableLocationDialog()
        } else {
            fetchDeviceLocation()
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Services Disabled")
            .setMessage("Please enable location services to refresh and view accurate location data.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                updateUI(latLng)
            }
        }
    }

    private fun loadAvatarLetter() {
        if (childId.isEmpty()) return
        firestore.collection("users").document(childId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                findViewById<TextView>(R.id.txtAvatarLetter)?.text = name.take(1).uppercase()
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.isZoomControlsEnabled = false
        mMap?.uiSettings?.isMyLocationButtonEnabled = false

        if (childId.isNotEmpty()) {
            firestore.collection("childLocations").document(childId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val lat = snapshot.getDouble("lat") ?: 0.0
                        val lng = snapshot.getDouble("lng") ?: 0.0
                        val latLng = LatLng(lat, lng)
                        lastLatLng = latLng
                        updateUI(latLng)
                    }
                }
        }
    }

    private fun updateUI(latLng: LatLng) {
        mMap?.clear()
        mMap?.addMarker(MarkerOptions().position(latLng).title("Current Location"))
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@ChildLocationActivity, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                
                withContext(Dispatchers.Main) {
                    if (addresses?.isNotEmpty() == true) {
                        val addressLine = addresses[0].getAddressLine(0)
                        findViewById<TextView>(R.id.txtAddress)?.text = addressLine
                    } else {
                        findViewById<TextView>(R.id.txtAddress)?.text = "Address not found"
                    }
                    findViewById<TextView>(R.id.txtLastUpdated)?.text = "updated just now"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.txtAddress)?.text = "${latLng.latitude}, ${latLng.longitude}"
                }
            }
        }
    }
}
