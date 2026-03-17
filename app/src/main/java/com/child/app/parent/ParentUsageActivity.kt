package com.child.app.parent

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class ParentUsageActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var usageSummaryText: TextView
    private lateinit var recyclerApps: RecyclerView
    private var childUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_usage)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        usageSummaryText = findViewById(R.id.usageSummaryText)
        recyclerApps = findViewById(R.id.recyclerAppsUsage)
        childUid = intent.getStringExtra("childUid") ?: ""

        findViewById<android.view.View>(R.id.alertBtn).setOnClickListener {
            val intent = android.content.Intent(this, AlertActivity::class.java)
            intent.putExtra("childUid", childUid)
            startActivity(intent)
        }
        
        loadUsageStats()
    }

    private fun loadUsageStats() {
        if (childUid.isEmpty()) {
            usageSummaryText.text = "No child"
            return
        }

        firestore.collection("usage")
            .document(childUid)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->
                val appList = mutableListOf<Pair<String, Long>>()

                for (doc in result) {
                    val pkg = doc.id
                    val time = doc.getLong("time") ?: 0L
                    if (time > 0) {
                        appList.add(pkg to time)
                    }
                }

                appList.sortByDescending { it.second }

                var totalMs = 0L
                for ((_, time) in appList) {
                    totalMs += time
                }

                usageSummaryText.text = "${totalMs / 60000} min"
                
                val adapter = AppsAdapter(appList) { pkg ->
                    // Handle app click if needed
                }
                recyclerApps.adapter = adapter
            }
    }
}
