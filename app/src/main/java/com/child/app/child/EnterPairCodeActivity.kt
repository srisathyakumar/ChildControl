package com.child.app.child

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EnterPairCodeActivity : AppCompatActivity() {

    private lateinit var etCode: EditText
    private lateinit var etChildName: EditText
    private lateinit var btnPair: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_pair_code)

        etCode = findViewById(R.id.etPairCode)
        etChildName = findViewById(R.id.etChildName)
        btnPair = findViewById(R.id.btnPairNow)

        btnPair.setOnClickListener {
            verifyPairCode()
        }
    }

    private fun verifyPairCode() {

        val code = etCode.text.toString().trim()
        val childName = etChildName.text.toString().trim()

        if (childName.isEmpty()) {
            etChildName.error = "Enter child name"
            return
        }
        val childId = auth.currentUser?.uid ?: return

        if (code.isEmpty()) {
            etCode.error = "Enter pairing code"
            return
        }

        firestore.collection("pairingCodes")
            .document(code)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {

                    Toast.makeText(
                        this,
                        "Invalid Pair Code",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val parentId =
                    doc.getString("parentId")
                        ?: return@addOnSuccessListener

                checkDuplicatePairing(
                    parentId,
                    childId,
                    code
                )
            }
    }

    // ✅ STEP 1 — Check duplicate
    private fun checkDuplicatePairing(
        parentId: String,
        childId: String,
        code: String
    ) {

        val pairingId = "${parentId}_${childId}"

        firestore.collection("pairings")
            .document(pairingId)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {

                    Toast.makeText(
                        this,
                        "Already Paired",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    createPairing(
                        parentId = parentId,
                        childId = childId,
                        code = code
                    )
                }
            }
    }

    // ✅ STEP 2 — Create pairing safely
    private fun createPairing(
        parentId: String,
        childId: String,
        code: String
    ) {

        val childName =
            etChildName.text.toString().trim()

        if (childName.isEmpty()) {
            etChildName.error = "Enter child name"
            return
        }

        val data = hashMapOf(
            "parentId" to parentId,
            "childId" to childId,
            "childName" to childName,   // ✅ NEW
            "status" to "active",
            "createdAt" to Date()
        )

        firestore.collection("pairings")
            .add(data)
            .addOnSuccessListener {

                // 🔥 SAVE ROLE + IDS LOCALLY
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

                prefs.edit()
                    .putString("role", "child")
                    .putString("childUid", childId)
                    .putString("parentUid", parentId)
                    .apply()

                firestore.collection("pairingCodes")
                    .document(code)
                    .delete()

                Toast.makeText(
                    this,
                    "Device Paired Successfully",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
    }
}