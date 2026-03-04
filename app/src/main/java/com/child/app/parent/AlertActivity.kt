package com.child.app.parent

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_alert)

        val alertText = findViewById<TextView>(R.id.alertText)

        val childUid = intent.getStringExtra("childUid")!!

        FirebaseFirestore.getInstance()
            .collection("alerts")
            .document(childUid)
            .addSnapshotListener { doc, _ ->

                if (doc == null || !doc.exists()) return@addSnapshotListener

                val alert = doc.getString("alert")

                if (alert == "emergency") {
                    alertText.text = "🚨 Emergency Alert from Child!"
                }

                if (alert == "geofence_violation") {
                    alertText.text = "⚠ Child left the GeoFence area!"
                }
            }
    }
}