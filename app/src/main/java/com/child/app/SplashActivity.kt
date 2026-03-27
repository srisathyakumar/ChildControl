package com.child.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.child.app.child.ChildDashboardActivity
import com.child.app.child.EnterPairCodeActivity
import com.child.app.parent.GeneratePairCodeActivity
import com.child.app.parent.ParentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set Kidly logo splash screen
        setContentView(R.layout.activity_main) 

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            checkUserRoleAndRedirect(user.uid)
        }
    }

    private fun checkUserRoleAndRedirect(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role")?.lowercase()
                    if (role != null) {
                        checkPairingAndRedirect(uid, role)
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }

    private fun checkPairingAndRedirect(uid: String, role: String) {
        val pairingQuery = if (role == "parent") {
            FirebaseFirestore.getInstance().collection("pairings").whereEqualTo("parentId", uid)
        } else {
            FirebaseFirestore.getInstance().collection("pairings").whereEqualTo("childId", uid)
        }

        pairingQuery.limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                val isPaired = !querySnapshot.isEmpty
                val intent = when (role) {
                    "parent" -> {
                        if (isPaired) Intent(this, ParentDashboardActivity::class.java)
                        else Intent(this, GeneratePairCodeActivity::class.java)
                    }
                    "child" -> {
                        if (isPaired) Intent(this, ChildDashboardActivity::class.java)
                        else Intent(this, EnterPairCodeActivity::class.java)
                    }
                    else -> Intent(this, MainActivity::class.java)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
}
