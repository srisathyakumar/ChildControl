package com.child.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.child.app.child.ChildDashboardActivity
import com.child.app.parent.ParentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostLoginRedirectActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "Redirect"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_login_redirect)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            Log.e(TAG, "No authenticated user found")
            goToLogin()
            return
        }

        Log.d(TAG, "Fetching user document for UID: ${user.uid}")

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (isFinishing) return@addOnSuccessListener

                if (!doc.exists()) {
                    Log.e(TAG, "User document does not exist for UID: ${user.uid}")
                    showErrorAndLogout("User profile not found in database. Please register a new account.")
                    return@addOnSuccessListener
                }

                val roleField = doc.getString("role")
                if (roleField == null) {
                    Log.e(TAG, "Role field is missing for UID: ${user.uid}")
                    showErrorAndLogout("User role is missing in your profile. Please register again.")
                    return@addOnSuccessListener
                }

                val role = roleField.trim().lowercase()
                Log.d(TAG, "User role found: $role")

                val intent = when (role) {
                    "parent" -> Intent(this, ParentDashboardActivity::class.java)
                    "child" -> Intent(this, ChildDashboardActivity::class.java)
                    else -> {
                        Log.e(TAG, "Invalid role value: $role")
                        showErrorAndLogout("Invalid role assigned ($role). Please contact support.")
                        null
                    }
                }

                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                if (isFinishing) return@addOnFailureListener
                Log.e(TAG, "Firestore fetch failed", e)
                Toast.makeText(this, "Connection Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                // Return to login after showing error
                window.decorView.postDelayed({
                    if (!isFinishing) goToLogin()
                }, 3000)
            }
    }

    private fun showErrorAndLogout(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        auth.signOut()
        window.decorView.postDelayed({
            if (!isFinishing) goToLogin()
        }, 3500)
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
