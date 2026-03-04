package com.child.app.parent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore

class ParentMapActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_parent_map)

        val childUid = intent.getStringExtra("childUid")!!

        val mapFragment =
            supportFragmentManager
                .findFragmentById(R.id.map)
                    as SupportMapFragment

        mapFragment.getMapAsync { map ->

            firestore.collection("locations")
                .document(childUid)
                .addSnapshotListener { doc, _ ->

                    if (doc == null) return@addSnapshotListener

                    val lat = doc.getDouble("lat") ?: return@addSnapshotListener
                    val lng = doc.getDouble("lng") ?: return@addSnapshotListener

                    val pos = LatLng(lat, lng)

                    map.clear()

                    map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Child Location")
                    )

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(pos, 15f)
                    )
                }
        }
    }
}