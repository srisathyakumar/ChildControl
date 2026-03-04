package com.child.app.child

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import com.child.app.R
import com.child.app.child.ChildUsageUploader

class ChildDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)
        startService(Intent(this, LocationService::class.java))
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        val btnPair = findViewById<Button>(R.id.btnEnterCode)

        btnPair.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    EnterPairCodeActivity::class.java
                )
            )
        }

        val btnUsage = findViewById<Button>(R.id.btnUsage)

        btnUsage.setOnClickListener {
            startActivity(
                Intent(this, UsageStatsActivity::class.java)
            )
        }

        val uploader = ChildUsageUploader(this)
        uploader.uploadTodayUsage()

        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

        findViewById<Button>(R.id.emergencyBtn).setOnClickListener {

            val prefs =
                getSharedPreferences("app_prefs", MODE_PRIVATE)

            val childUid = prefs.getString("childUid", null) ?: return@setOnClickListener

            FirebaseFirestore.getInstance()
                .collection("alerts")
                .document(childUid)
                .set(
                    mapOf(
                        "type" to "emergency",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
        }

        val emergencyBtn = findViewById<Button>(R.id.emergencyBtn)

        emergencyBtn.setOnClickListener {

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val childUid = prefs.getString("childUid", null) ?: return@setOnClickListener

            val data = hashMapOf(
                "alert" to "emergency",
                "timestamp" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("alerts")
                .document(childUid)
                .set(data)
        }
    }
}
