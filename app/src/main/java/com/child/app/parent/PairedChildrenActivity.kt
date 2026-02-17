package com.child.app.parent

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PairedChildrenActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PairedChildrenAdapter
    private val childList =
        mutableListOf<Pair<String,String>>()
// childId , childName
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paired_children)

        recyclerView = findViewById(R.id.recyclerChildren)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        adapter =
            PairedChildrenAdapter(childList) { childId ->

                val intent = Intent(
                    this,
                    SetScreenLimitActivity::class.java
                )

                intent.putExtra("childId", childId)
                startActivity(intent)
            }
        recyclerView.adapter = adapter

        loadPairedChildren()
    }

    private fun loadPairedChildren() {

        val parentId = auth.currentUser?.uid ?: return

        Toast.makeText(
            this,
            "Parent UID: $parentId",
            Toast.LENGTH_LONG
        ).show()

        firestore.collection("pairings")
            .get()
            .addOnSuccessListener { result ->

                childList.clear()

                val uniqueMap = mutableMapOf<String, String>()
                // childUid -> childName

                for (doc in result) {

                    val pId = doc.getString("parentId")
                    val cId = doc.getString("childId")
                    val cName = doc.getString("childName") ?: "Unknown"

                    if (pId == parentId && cId != null) {
                        uniqueMap[cId] = cName
                    }
                }

                // Convert map → list of pairs
                for ((uid, name) in uniqueMap) {
                    childList.add(Pair(uid, name))
                }

                adapter.notifyDataSetChanged()

                if (childList.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No paired children found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}
