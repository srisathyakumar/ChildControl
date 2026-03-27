package com.child.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.child.app.child.EnterPairCodeActivity
import com.child.app.parent.GeneratePairCodeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvRoleHint: TextView

    private var selectedRole: String? = null // "parent" or "child"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvRoleHint = findViewById(R.id.tvRoleHint)

        setupRoleSelection()

        btnRegister.setOnClickListener {
            registerUser()
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupRoleSelection() {
        val parentLeft = findViewById<View>(R.id.viewParentLeft)
        val parentRight = findViewById<View>(R.id.viewParentRight)
        val childView = findViewById<View>(R.id.viewChild)

        val parentClickListener = View.OnClickListener {
            selectedRole = "parent"
            tvRoleHint.text = "Role Selected: Parent"
            tvRoleHint.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
        }

        parentLeft.setOnClickListener(parentClickListener)
        parentRight.setOnClickListener(parentClickListener)

        childView.setOnClickListener {
            selectedRole = "child"
            tvRoleHint.text = "Role Selected: Child"
            tvRoleHint.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }
    }

    private fun registerUser() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty()) {
            etFullName.error = "Name required"
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid email required"
            return
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }
        if (selectedRole == null) {
            Toast.makeText(this, "Please tap on the image to select your role", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                    saveUserToFirestore(uid, name, email, selectedRole!!)
                } else {
                    btnRegister.isEnabled = true
                    val message = "Registration Failed: ${task.exception?.localizedMessage ?: "Unknown error"}"
                    Log.e("RegisterActivity", "Error", task.exception)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, role: String) {
        // Generate a random dynamic color for the profile background
        val randomColor = String.format("#%06X", 0xFFFFFF and Color.rgb(
            Random.nextInt(50, 200),
            Random.nextInt(50, 200),
            Random.nextInt(50, 200)
        ))

        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "role" to role,
            "avatarColor" to randomColor
        )
        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show()
                val intent = when (role) {
                    "parent" -> Intent(this, GeneratePairCodeActivity::class.java)
                    "child" -> Intent(this, EnterPairCodeActivity::class.java)
                    else -> Intent(this, LoginActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnRegister.isEnabled = true
                Toast.makeText(this, "Firestore Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
