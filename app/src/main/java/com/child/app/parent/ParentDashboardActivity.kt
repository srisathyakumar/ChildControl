package com.child.app.parent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.child.app.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ParentDashboardActivity : AppCompatActivity() {

    private val TAG = "ParentDashboard"
    private var selectedChildUid: String? = null
    private var miniMap: GoogleMap? = null
    private var locationListener: ListenerRegistration? = null
    private var usageListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        val user = FirebaseAuth.getInstance().currentUser ?: run {
            goToLogin()
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

        setupProfileHeader(user.uid)
        setupNavigation()
        setupDashboardActions()
        setupMiniMap()
        
        fetchFirstChild(user.uid)
    }

    private fun setupMiniMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            miniMap = googleMap
            miniMap?.uiSettings?.apply {
                isMyLocationButtonEnabled = false
                isMapToolbarEnabled = false
            }
        }
    }

    private fun fetchFirstChild(parentId: String) {
        FirebaseFirestore.getInstance().collection("pairings")
            .whereEqualTo("parentId", parentId)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                if (doc != null) {
                    selectedChildUid = doc.getString("childId")
                    val childName = doc.getString("childName") ?: "Child"
                    findViewById<TextView>(R.id.txtChildHeaderName)?.text = childName
                    findViewById<TextView>(R.id.txtAvatarLetter)?.text = childName.take(1).uppercase()
                    selectedChildUid?.let { uid ->
                        listenToChildUsage(uid)
                        listenToChildLocation(uid)
                    }
                }
            }
    }

    private fun listenToChildLocation(childId: String) {
        locationListener?.remove()
        locationListener = FirebaseFirestore.getInstance()
            .collection("childLocations")
            .document(childId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val lat = snapshot.getDouble("lat") ?: return@addSnapshotListener
                val lng = snapshot.getDouble("lng") ?: return@addSnapshotListener
                val latLng = LatLng(lat, lng)

                miniMap?.let { map ->
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
                
                findViewById<TextView>(R.id.txtLocationStatus)?.text = "Live Location Updated"
            }
    }

    private fun listenToChildUsage(childId: String) {
        usageListener?.remove()
        usageListener = FirebaseFirestore.getInstance().collection("usage").document(childId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    findViewById<TextView>(R.id.screenTimeText)?.text = "0 min"
                    return@addSnapshotListener
                }
                
                val totalTimeMs = snapshot.getLong("totalTime") ?: 0L
                val minutes = totalTimeMs / 60000
                val h = minutes / 60
                val m = minutes % 60
                
                findViewById<TextView>(R.id.screenTimeText)?.text = if (h > 0) "${h}h ${m}m" else "${m} min"
            }
    }

    private fun setupDashboardActions() {
        findViewById<View>(R.id.usageBtn)?.setOnClickListener {
            val intent = Intent(this, ParentUsageActivity::class.java)
            intent.putExtra("childUid", selectedChildUid)
            startActivity(intent)
        }

        findViewById<View>(R.id.locationCard)?.setOnClickListener {
            val intent = Intent(this, ParentMapActivity::class.java)
            intent.putExtra("childUid", selectedChildUid)
            startActivity(intent)
        }

        findViewById<MaterialSwitch>(R.id.btnLockDeviceSwitch)?.setOnCheckedChangeListener { _, isChecked ->
            val command = if (isChecked) "lock" else "unlock"
            selectedChildUid?.let { uid ->
                FirebaseFirestore.getInstance().collection("deviceCommands").document(uid)
                    .set(mapOf("command" to command))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Device ${if (isChecked) "locked" else "unlocked"}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        findViewById<View>(R.id.btnLimits)?.setOnClickListener {
            openControls()
        }
        
        findViewById<View>(R.id.childSelector)?.setOnClickListener {
            startActivity(Intent(this, PairedChildrenActivity::class.java))
        }
    }

    private fun openControls() {
        if (selectedChildUid == null) {
            Toast.makeText(this, "No child selected", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ChildControlActivity::class.java)
        intent.putExtra("childUid", selectedChildUid)
        startActivity(intent)
    }

    private fun setupProfileHeader(uid: String) {
        // Fetch parent's name from their user profile
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val parentName = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "Parent"
            }

        val avatar = findViewById<View>(R.id.avatarContainer)
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
                R.id.nav_logout -> {
                    logout()
                    true
                }
                R.id.nav_app_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupNavigation() {
        findViewById<BottomNavigationView>(R.id.bottomNav)?.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> true
                R.id.nav_controls -> {
                    openControls()
                    true
                }
                R.id.nav_location -> {
                    val intent = Intent(this, ParentMapActivity::class.java)
                    intent.putExtra("childUid", selectedChildUid)
                    startActivity(intent)
                    true
                }
                R.id.nav_stats -> {
                    val intent = Intent(this, ParentUsageActivity::class.java)
                    intent.putExtra("childUid", selectedChildUid)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
        usageListener?.remove()
    }
}
