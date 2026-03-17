package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.child.app.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ParentDashboardActivity : AppCompatActivity() {

    private val TAG = "ParentDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        val user = FirebaseAuth.getInstance().currentUser ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupProfileHeader(user.uid)
        setupButtons()
        setupNavigation()
    }

    private fun setupButtons() {
        // Child selector (Satya pill)
        findViewById<View>(R.id.childSelector)?.setOnClickListener {
            // Logic to switch between children if multiple exist
            Toast.makeText(this, "Child selector clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.usageBtn)?.setOnClickListener {
            startActivity(Intent(this, ParentUsageActivity::class.java))
        }

        findViewById<View>(R.id.geoFenceBtn)?.setOnClickListener {
            startActivity(Intent(this, SetGeoFenceActivity::class.java))
        }

        findViewById<View>(R.id.btnLockDevice)?.setOnClickListener {
            // Implementation for locking the child's device
            Toast.makeText(this, "Lock command sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProfileHeader(uid: String) {
        val avatar = findViewById<View>(R.id.avatarContainer)
        val txtLetter = findViewById<TextView>(R.id.txtAvatarLetter)
        val txtName = findViewById<TextView>(R.id.txtChildHeaderName)

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isFinishing) return@addOnSuccessListener
                val name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "User"
                txtName?.text = name
                txtLetter?.text = name.take(1).uppercase()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching profile", e)
            }

        avatar?.setOnClickListener { showProfileMenu(it) }
    }

    private fun showProfileMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_app_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_help -> {
                    startActivity(Intent(this, HelpActivity::class.java))
                    true
                }
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupNavigation() {
        findViewById<BottomNavigationView>(R.id.bottomNav)?.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_controls -> {
                    // Logic for controls view
                    true
                }
                R.id.nav_location -> {
                    startActivity(Intent(this, ParentMapActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
