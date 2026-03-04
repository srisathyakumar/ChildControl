package com.child.app.parent

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class ParentUsageActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_usage)

        val childUid = intent.getStringExtra("childUid") ?: return

        val textView = findViewById<TextView>(R.id.usageText)

        firestore.collection("usage")
            .document(childUid)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->

                val builder = StringBuilder()

                val appList = mutableListOf<Pair<String, Long>>()

                for (doc in result) {
                    val pkg = doc.id
                    val time = doc.getLong("timeUsed") ?: 0
                    if (time > 0) {
                        appList.add(pkg to time)
                    }
                }

                appList.sortByDescending { it.second }

                val totalMinutes = appList.sumOf { it.second } / 60000
                builder.append("Total Screen Time: $totalMinutes min\n\n")

                for ((pkg, time) in appList) {
                    val minutes = time / 60000
                    builder.append("$pkg : $minutes min\n")
                }

                textView.text = builder.toString()
            }

        val geoFenceBtn = findViewById<Button>(R.id.geoFenceBtn)

        geoFenceBtn.setOnClickListener {

            val intent = Intent(this, SetGeoFenceActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        val mapBtn = findViewById<Button>(R.id.mapBtn)

        mapBtn.setOnClickListener {

            val intent = Intent(this, ParentMapActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

        val alertBtn = findViewById<Button>(R.id.alertBtn)

        alertBtn.setOnClickListener {

            val intent = Intent(this, AlertActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }

    }
}
