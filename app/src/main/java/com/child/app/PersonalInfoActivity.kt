package com.child.app

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalInfoActivity : AppCompatActivity() {

    private var txtAvatarPreview: TextView? = null
    private var txtName: TextView? = null
    private var txtEmail: TextView? = null
    private var txtGender: TextView? = null
    private var txtPhone: TextView? = null
    private var txtBirthday: TextView? = null
    private var txtLanguage: TextView? = null
    private var txtHomeAddress: TextView? = null
    private var txtWorkAddress: TextView? = null
    private var txtPasswordStatus: TextView? = null
    private var btnBack: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.personalinfo)
            initViews()
            btnBack?.setOnClickListener { finish() }
            loadUserProfile()
        } catch (e: Exception) {
            Log.e("PersonalInfoActivity", "Error in onCreate", e)
            finish()
        }
    }

    private fun initViews() {
        txtAvatarPreview = findViewById(R.id.txtAvatarPreview)
        txtName = findViewById(R.id.txtName)
        txtEmail = findViewById(R.id.txtEmail)
        txtGender = findViewById(R.id.txtGender)
        txtPhone = findViewById(R.id.txtPhone)
        txtBirthday = findViewById(R.id.txtBirthday)
        txtLanguage = findViewById(R.id.txtLanguage)
        txtHomeAddress = findViewById(R.id.txtHomeAddress)
        txtWorkAddress = findViewById(R.id.txtWorkAddress)
        txtPasswordStatus = findViewById(R.id.txtPasswordStatus)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                
                val name = doc.getString("name") ?: "User"
                val email = doc.getString("email") ?: ""
                
                txtName?.text = name
                txtEmail?.text = email
                txtAvatarPreview?.text = name.take(1).uppercase()
                
                // Optional fields
                doc.getString("gender")?.let { txtGender?.text = it }
                doc.getString("phone")?.let { txtPhone?.text = it }
                doc.getString("birthday")?.let { txtBirthday?.text = it }
                doc.getString("language")?.let { txtLanguage?.text = it }
                doc.getString("homeAddress")?.let { txtHomeAddress?.text = it }
                doc.getString("workAddress")?.let { txtWorkAddress?.text = it }
            }
            .addOnFailureListener { e ->
                Log.e("PersonalInfoActivity", "Error loading profile", e)
            }
    }
}
