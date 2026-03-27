package com.child.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.child.app.child.ChildDashboardActivity
import com.child.app.child.EnterPairCodeActivity
import com.child.app.parent.GeneratePairCodeActivity
import com.child.app.parent.ParentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser != null) {
            handleUserRedirect()
            return
        }

        findViewById<View>(R.id.main).let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updatePadding(
                    left = systemBars.left,
                    top = systemBars.top,
                    right = systemBars.right,
                    bottom = systemBars.bottom
                )
                insets
            }
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            loginUser()
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid email required"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password required"
            return
        }

        btnLogin.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    handleUserRedirect()
                } else {
                    btnLogin.isEnabled = true
                    val exception = task.exception
                    val message = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                        else -> "Login Failed: ${exception?.localizedMessage ?: "Internal Error"}"
                    }
                    Log.e("LoginActivity", "Login Error", exception)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleUserRedirect() {
        val user = auth.currentUser ?: return
        
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val role = doc.getString("role")?.trim()?.lowercase() ?: ""
                    checkPairingAndRedirect(user.uid, role)
                } else {
                    auth.signOut()
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Connection Error", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
            }
    }

    private fun checkPairingAndRedirect(uid: String, role: String) {
        val pairingQuery = if (role == "parent") {
            firestore.collection("pairings").whereEqualTo("parentId", uid)
        } else {
            firestore.collection("pairings").whereEqualTo("childId", uid)
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
                    else -> null
                }

                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    auth.signOut()
                    Toast.makeText(this, "Invalid role", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking pairing", Toast.LENGTH_SHORT).show()
                btnLogin.isEnabled = true
            }
    }
}
