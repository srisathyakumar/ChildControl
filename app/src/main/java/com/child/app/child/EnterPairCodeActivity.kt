package com.child.app.child

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var btnPair: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "EnterPairCode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_pair_code)

        etCode = findViewById(R.id.etPairCode)
        btnPair = findViewById(R.id.btnPairNow)

        btnPair.setOnClickListener {
            verifyPairCode()
        }
    }

    private fun verifyPairCode() {
        val code = etCode.text.toString().trim()

        if (code.isEmpty()) {
            etCode.error = "Enter pairing code"
            return
        }

        val childId = auth.currentUser?.uid ?: return

        btnPair.isEnabled = false

        firestore.collection("pairingCodes")
            .document(code)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    btnPair.isEnabled = true
                    Toast.makeText(this, "Invalid Pair Code", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val parentId = doc.getString("parentId")
                if (parentId == null) {
                    btnPair.isEnabled = true
                    return@addOnSuccessListener
                }

                checkPairingLimitsAndCreate(parentId, childId, code)
            }
            .addOnFailureListener { e ->
                btnPair.isEnabled = true
                Log.e(TAG, "Error verifying code", e)
                Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPairingLimitsAndCreate(parentId: String, childId: String, code: String) {
        firestore.collection("pairings")
            .whereEqualTo("childId", childId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingPairings = querySnapshot.documents
                
                val alreadyPairedWithThisParent = existingPairings.any { it.getString("parentId") == parentId }
                if (alreadyPairedWithThisParent) {
                    btnPair.isEnabled = true
                    Toast.makeText(this, "Already paired with this parent", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                if (existingPairings.size >= 2) {
                    btnPair.isEnabled = true
                    Toast.makeText(this, "Child can only be paired with up to 2 parents", Toast.LENGTH_LONG).show()
                } else {
                    fetchChildNameAndCreate(parentId, childId, code)
                }
            }
            .addOnFailureListener { e ->
                btnPair.isEnabled = true
                Log.e(TAG, "Error checking limits", e)
            }
    }

    private fun fetchChildNameAndCreate(parentId: String, childId: String, code: String) {
        firestore.collection("users").document(childId).get()
            .addOnSuccessListener { doc ->
                val childName = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "Child"
                createPairing(parentId, childId, code, childName)
            }
            .addOnFailureListener {
                createPairing(parentId, childId, code, "Child")
            }
    }

    private fun createPairing(parentId: String, childId: String, code: String, childName: String) {
        val pairingId = "${parentId}_${childId}"

        val data = hashMapOf(
            "parentId" to parentId,
            "childId" to childId,
            "childName" to childName,
            "status" to "active",
            "createdAt" to Date()
        )

        firestore.collection("pairings")
            .document(pairingId)
            .set(data)
            .addOnSuccessListener {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit()
                    .putString("role", "child")
                    .putString("childUid", childId)
                    .putString("parentUid", parentId)
                    .apply()

                firestore.collection("pairingCodes").document(code).delete()

                Toast.makeText(this, "Device Paired Successfully", Toast.LENGTH_LONG).show()
                
                // Redirect to ChildDashboard instead of finishing
                val intent = Intent(this, ChildDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnPair.isEnabled = true
                Log.e(TAG, "Error creating pairing", e)
                Toast.makeText(this, "Pairing failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
