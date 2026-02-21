package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class ChildAppsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AppsAdapter

    // packageName + timeUsed
    private val appsList =
        mutableListOf<Pair<String, Long>>()

    private val firestore =
        FirebaseFirestore.getInstance()

    private lateinit var childId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_apps)

        // ✅ RECEIVE CORRECT KEY
        childId =
            intent.getStringExtra("childId") ?: ""

        // ✅ Safety check
        if (childId.isEmpty()) {
            Toast.makeText(
                this,
                "Child ID missing",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        recycler =
            findViewById(R.id.recyclerApps)

        recycler.layoutManager =
            LinearLayoutManager(this)

        adapter =
            AppsAdapter(appsList) { packageName ->

                openSetLimit(packageName)
            }

        recycler.adapter = adapter

        loadApps()
    }

    // 🔥 Load usage apps from Firebase
    private fun loadApps() {

        firestore.collection("usage")
            .document(childId)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->

                appsList.clear()

                for (doc in result) {

                    val pkg =
                        doc.getString("appPackage") ?: continue

                    val time =
                        doc.getLong("timeUsed") ?: 0

                    // ✅ FILTER SYSTEM APPS
                    if (isSystemApp(pkg)) continue

                    appsList.add(Pair(pkg, time))
                }

                adapter.notifyDataSetChanged()

                if (appsList.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No usage data found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun isSystemApp(packageName: String): Boolean {

        return packageName.contains("launcher") ||
                packageName.contains("systemui") ||
                packageName.contains("settings") ||
                packageName.contains("telecom") ||
                packageName.contains("permissioncontroller") ||
                packageName.contains("packageinstaller") ||
                packageName.startsWith("com.android.system") ||
                packageName.startsWith("android") ||
                packageName == "com.child.app" // hide own app
    }

    // 🔒 Open limit screen
    private fun openSetLimit(
        packageName: String
    ) {

        val intent = Intent(
            this,
            SetScreenLimitActivity::class.java
        )

        intent.putExtra("childId", childId)
        intent.putExtra("packageName", packageName)

        startActivity(intent)
    }
}