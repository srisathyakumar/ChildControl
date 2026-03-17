package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.child.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

class GeneratePairCodeActivity : AppCompatActivity() {

    private lateinit var tvCode: TextView
    private lateinit var btnGenerate: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var pairingListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_pair_code)

        tvCode = findViewById(R.id.tvPairCode)
        btnGenerate = findViewById(R.id.btnGenerateCode)

        btnGenerate.setOnClickListener {
            generatePairCode()
        }

        startPairingListener()
    }

    private fun startPairingListener() {
        val parentId = auth.currentUser?.uid ?: return
        
        // Listen for any successful pairing involving this parent
        pairingListener = firestore.collection("pairings")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null && !snapshot.isEmpty) {
                    Toast.makeText(this, "Pairing Successful!", Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, ParentDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun generatePairCode() {
        val parentId = auth.currentUser?.uid ?: return

        // Generate 6-digit code
        val code = (100000..999999).random().toString()

        val data = hashMapOf(
            "parentId" to parentId,
            "status" to "active",
            "createdAt" to Date()
        )

        firestore.collection("pairingCodes")
            .document(code)
            .set(data)
            .addOnSuccessListener {
                tvCode.text = code
                Toast.makeText(this, "Pairing Code Generated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to generate code", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        pairingListener?.remove()
    }
}
