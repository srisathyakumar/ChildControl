package com.child.app.parent

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.firestore.FirebaseFirestore

class ParentUsageActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_usage)

        val childUid = intent.getStringExtra("childUid")!!

        val textView = findViewById<TextView>(R.id.usageText)

        firestore.collection("usage")
            .document(childUid)
            .collection("apps")
            .get()
            .addOnSuccessListener { result ->

                val builder = StringBuilder()

                for (doc in result) {

                    val app = doc.id
                    val time = doc.getLong("timeUsed") ?: 0

                    builder.append("$app : $time sec\n")
                }

                textView.text = builder.toString()
            }
    }
}
