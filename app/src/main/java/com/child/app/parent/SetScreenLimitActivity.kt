package com.child.app.parent

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SetScreenLimitActivity : AppCompatActivity() {

    private lateinit var etPackage: EditText
    private lateinit var etMinutes: EditText
    private lateinit var btnSave: Button
    private lateinit var childId: String

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_screen_limit)
        childId =
            intent.getStringExtra("childId") ?: return

        etPackage = findViewById(R.id.etPackageName)
        etMinutes = findViewById(R.id.etLimitMinutes)
        btnSave = findViewById(R.id.btnSaveLimit)

        btnSave.setOnClickListener {
            saveLimit()
        }

        val pkg = intent.getStringExtra("packageName")

        if (pkg != null) {
            etPackage.setText(pkg)
        }
    }

    private fun saveLimit() {

        val packageName =
            etPackage.text.toString().trim()

        val minutes =
            etMinutes.text.toString().trim()

        val parentId =
            auth.currentUser?.uid ?: return

        val childId =
            intent.getStringExtra("childId")
                ?: return

        if (packageName.isEmpty() ||
            minutes.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Fill all fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val limit = minutes.toInt()

        val docId =
            "${parentId}_${childId}_${packageName}"

        val data = hashMapOf(
            "parentId" to parentId,
            "childId" to childId,
            "packageName" to packageName,
            "limitMinutes" to limit,
            "createdAt" to Date()
        )

        firestore.collection("screenLimits")
            .document(docId)
            .set(data)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Limit Saved",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
    }
}