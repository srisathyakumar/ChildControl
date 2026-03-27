package com.child.app.parent

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Using a generic layout or standard Material components if layout is removed
        // For now, I'll keep the Activity but you'll need to provide a new UI or remove it from Manifest
        setContentView(android.R.layout.simple_list_item_1)

        val childUid = intent.getStringExtra("childUid") ?: return

        FirebaseFirestore.getInstance()
            .collection("alerts")
            .document(childUid)
            .addSnapshotListener { doc, _ ->
                if (doc == null || !doc.exists()) return@addSnapshotListener
                val alert = doc.getString("alert")
                // Update UI logic here
            }
    }
}