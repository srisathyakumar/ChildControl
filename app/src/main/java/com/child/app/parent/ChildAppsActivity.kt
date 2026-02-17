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
    private val appsList = mutableListOf<Pair<String, Long>>() // package + time

    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var childId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_apps)

        childId = intent.getStringExtra("childUid") ?: return

        recycler = findViewById(R.id.recyclerApps)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AppsAdapter(appsList) { packageName ->
            openSetLimit(packageName)
        }

        recycler.adapter = adapter

        loadApps()
    }

    private fun loadApps() {

        firestore.collection("usage")
            .document(childId)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->

                appsList.clear()

                for (doc in result) {

                    val pkg = doc.getString("appPackage") ?: continue
                    val time = doc.getLong("timeUsed") ?: 0

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

    private fun openSetLimit(packageName: String) {

        val intent = Intent(
            this,
            SetScreenLimitActivity::class.java
        )

        intent.putExtra("childId", childId)
        intent.putExtra("packageName", packageName)

        startActivity(intent)
    }
}
