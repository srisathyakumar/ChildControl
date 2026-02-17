package com.child.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnRegister = findViewById(R.id.btnRegister)

        // Role dropdown
        val roles = arrayOf("Parent", "Child")
        spinnerRole.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val role = spinnerRole.selectedItem.toString()

        if (email.isEmpty()) {
            etEmail.error = "Enter email"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email"
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password must be 6+ chars"
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user!!.uid

                val userMap = hashMapOf(
                    "email" to email,
                    "role" to role
                )

                firestore.collection("users")
                    .document(uid)
                    .set(userMap)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Registered Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(
                                this,
                                LoginActivity::class.java
                            )
                        )
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
