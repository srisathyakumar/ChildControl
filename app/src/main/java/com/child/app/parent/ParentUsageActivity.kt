package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class ParentUsageActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var usageText: TextView
    private var childUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_usage)

        usageText = findViewById(R.id.usageText)
        childUid = intent.getStringExtra("childUid") ?: ""

        val geoFenceBtn = findViewById<Button>(R.id.geoFenceBtn)
        val mapBtn = findViewById<Button>(R.id.mapBtn)
        val alertBtn = findViewById<Button>(R.id.alertBtn)

        geoFenceBtn.setOnClickListener {
            val intent = Intent(this, SetGeoFenceActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        mapBtn.setOnClickListener {
            val intent = Intent(this, ParentMapActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        alertBtn.setOnClickListener {
            val intent = Intent(this, AlertActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }
        
        loadUsageStats()
    }

    private fun loadUsageStats() {
        if (childUid.isEmpty()) {
            usageText.text = "No child selected"
            return
        }

        firestore.collection("usage")
            .document(childUid)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->
                val builder = StringBuilder()
                val appList = mutableListOf<Pair<String, Long>>()

                for (doc in result) {
                    val appName = doc.getString("appName") ?: doc.id
                    val time = doc.getLong("time") ?: 0L
                    if (time > 0) {
                        appList.add(appName to time)
                    }
                }

                appList.sortByDescending { it.second }

                var totalMs = 0L
                val items = StringBuilder()
                for ((name, time) in appList) {
                    totalMs += time
                    val minutes = time / 60000
                    items.append("$name: $minutes min\n")
                }

                builder.append("Total Screen Time: ${totalMs / 60000} min\n\n")
                builder.append(items)

                usageText.text = builder.toString()
            }
            .addOnFailureListener {
                usageText.text = "Error loading usage: ${it.message}"
            }
    }
}
