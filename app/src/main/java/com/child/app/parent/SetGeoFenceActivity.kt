package com.child.app.parent

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import android.location.Location
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

class SetGeoFenceActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_geofence)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val childUid = intent.getStringExtra("childUid") ?: return

        val latInput = findViewById<EditText>(R.id.latitudeInput)
        val lngInput = findViewById<EditText>(R.id.longitudeInput)
        val radiusInput = findViewById<EditText>(R.id.radiusInput)
        val durationInput = findViewById<EditText>(R.id.durationInput)
        val typeGroup = findViewById<RadioGroup>(R.id.typeGroup)
        val saveBtn = findViewById<Button>(R.id.saveFenceBtn)

        val getLocationBtn = findViewById<Button>(R.id.getLocationBtn)

        getLocationBtn.setOnClickListener {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->

                    if (location != null) {

                        latInput.setText(location.latitude.toString())
                        lngInput.setText(location.longitude.toString())

                        Toast.makeText(
                            this,
                            "Location filled automatically",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
        saveBtn.setOnClickListener {

            val lat = latInput.text.toString().toDoubleOrNull()
            val lng = lngInput.text.toString().toDoubleOrNull()
            val radius = radiusInput.text.toString().toIntOrNull()

            if (lat == null || lng == null || radius == null) {
                Toast.makeText(this, "Enter valid location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type =
                if (typeGroup.checkedRadioButtonId == R.id.staticFence)
                    "static"
                else
                    "temporary"

            val duration =
                durationInput.text.toString().toIntOrNull() ?: 0

            val data = hashMapOf(
                "latitude" to lat,
                "longitude" to lng,
                "radius" to radius,
                "type" to type,
                "duration" to duration,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("geofences")
                .document(childUid)
                .set(data)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Geo Fence Saved",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
        }
    }
}