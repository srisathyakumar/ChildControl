package com.child.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, PostLoginRedirectActivity::class.java))
            finish()
            return
        }

        // Show Welcome Page if not logged in
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btnContinue).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
