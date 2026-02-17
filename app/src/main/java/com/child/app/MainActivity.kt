package com.child.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optional: If you have splash layout use setContentView()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Small delay (Splash effect optional)
        Handler(Looper.getMainLooper()).postDelayed({

            val user = auth.currentUser

            if (user != null) {

                // 🔁 User already logged in
                startActivity(
                    Intent(
                        this,
                        PostLoginRedirectActivity::class.java
                    )
                )

            } else {

                // 🔐 Not logged in
                startActivity(
                    Intent(
                        this,
                        LoginActivity::class.java
                    )
                )
            }

            finish()

        }, 1000) // 1 second delay
    }
}
