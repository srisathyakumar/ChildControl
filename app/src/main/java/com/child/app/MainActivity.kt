package com.child.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        printSigningHash()

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            startActivity(Intent(this, PostLoginRedirectActivity::class.java))
            finish()
            return
        }

        // Show Welcome Page if not logged in
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btnLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<View>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun printSigningHash() {
        try {
            val packageName = packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val digest = md.digest()
                val hexString = digest.joinToString(":") { "%02X".format(it) }
                Log.d("SIGNING_HASH", "SHA-256: $hexString")
            }
        } catch (e: Exception) {
            Log.e("SIGNING_HASH", "Error getting signing hash", e)
        }
    }
}
