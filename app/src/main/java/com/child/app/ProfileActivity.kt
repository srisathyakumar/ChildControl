package com.child.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var txtAvatarLetter: TextView
    private lateinit var txtName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var btnClose: ImageView
    private lateinit var btnPersonalInfo: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        txtAvatarLetter = findViewById(R.id.txtAvatarLetter)
        txtName = findViewById(R.id.txtName)
        txtEmail = findViewById(R.id.txtEmail)
        btnClose = findViewById(R.id.btnClose)
        btnPersonalInfo = findViewById(R.id.btnPersonalInfo)

        btnClose.setOnClickListener { finish() }
        
        btnPersonalInfo.setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "User"
                val email = doc.getString("email") ?: ""
                
                txtName.text = name
                txtEmail.text = email
                txtAvatarLetter.text = name.take(1).uppercase()
            }
    }
}
