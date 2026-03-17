package com.child.app.child

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.child.app.HelpActivity
import com.child.app.InfoActivity
import com.child.app.ProfileActivity
import com.child.app.R
import com.child.app.parent.SettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class ChildDashboardActivity : AppCompatActivity() {

    private val TAG = "ChildDashboard"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "No user session found, finishing")
            finish()
            return
        }
        val childId = user.uid

        setupProfileHeader(childId)
        setupUI(childId)
        setupCommandListener(childId)
        AppBehaviourAnalyzer.checkInstallAbuse()
        checkPermissionsAndStartService()
        setupWorkManager()
    }

    private fun setupProfileHeader(uid: String) {
        val avatarContainer = findViewById<View>(R.id.avatarContainer)
        val txtAvatarLetter = findViewById<TextView>(R.id.txtAvatarLetter)
        val txtHeaderName = findViewById<TextView>(R.id.txtHeaderName)

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("email")?.split("@")?.get(0) ?: "User"
                txtHeaderName.text = name
                txtAvatarLetter.text = name.take(1).uppercase()
            }

        avatarContainer.setOnClickListener { view ->
            showProfileMenu(view)
        }
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

    private fun setupUI(childId: String) {
        try {
            val btnPair = findViewById<Button>(R.id.btnEnterCode)
            val btnUsage = findViewById<Button>(R.id.btnUsage)
            val emergencyBtn = findViewById<Button>(R.id.emergencyBtn)

            val locationStatus = findViewById<TextView>(R.id.locationStatus)
            val usageStatus = findViewById<TextView>(R.id.usageStatus)
            val accessibilityStatus = findViewById<TextView>(R.id.accessibilityStatus)

            locationStatus.text = "Location: Active"
            usageStatus.text = if (hasUsagePermission()) "Usage Monitoring: Active" else "Usage Monitoring: Disabled"
            accessibilityStatus.text = if (isAccessibilityEnabled()) "App Blocking: Active" else "App Blocking: Disabled"

            btnPair.setOnClickListener {
                startActivity(Intent(this, EnterPairCodeActivity::class.java))
            }

            btnUsage.setOnClickListener {
                startActivity(Intent(this, UsageStatsActivity::class.java))
            }

            emergencyBtn.setOnClickListener {
                sendEmergencyAlert(childId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "UI Setup failed - check layout IDs", e)
            Toast.makeText(this, "Internal UI Error", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCommandListener(childId: String) {
        FirebaseFirestore.getInstance()
            .collection("deviceCommands")
            .document(childId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Command listener failed", e)
                    return@addSnapshotListener
                }
                val command = snapshot?.getString("command") ?: return@addSnapshotListener
                if (command == "lock") {
                    val intent = Intent(this, PhoneLockedActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }
    }

    private fun checkPermissionsAndStartService() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startLocationService()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startLocationService() {
        try {
            val intent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocationService", e)
        }
    }

    private fun setupWorkManager() {
        val request = PeriodicWorkRequestBuilder<DailyAnalyticsWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "dailyAnalytics",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun sendEmergencyAlert(uid: String) {
        FirebaseFirestore.getInstance()
            .collection("alerts")
            .document(uid)
            .set(
                mapOf(
                    "type" to "emergency",
                    "childId" to uid,
                    "timestamp" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Emergency alert sent!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName)
    }
}
