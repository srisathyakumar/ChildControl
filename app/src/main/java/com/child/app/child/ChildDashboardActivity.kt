package com.child.app.child

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.child.app.LoginActivity
import com.child.app.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*
import java.util.concurrent.TimeUnit

class ChildDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private val TAG = "ChildDashboard"
    
    private lateinit var statsView: View
    private lateinit var risksView: View
    private lateinit var locationView: View
    private lateinit var txtHeaderName: TextView
    private lateinit var txtAvatarLetter: TextView
    private lateinit var txtScreenTime: TextView
    private lateinit var imgApp1: ImageView
    private lateinit var imgApp2: ImageView
    private lateinit var imgApp3: ImageView

    private lateinit var scanPulseView: View
    private lateinit var scanProgressIndicator: CircularProgressIndicator
    private lateinit var txtScanPercent: TextView
    private lateinit var imgScanStatus: ImageView
    private lateinit var btnScan: View
    private lateinit var rvMaliciousApps: RecyclerView
    private lateinit var txtRiskSummary: TextView

    private var miniMap: GoogleMap? = null
    private var fullMap: GoogleMap? = null
    private var locationListener: ListenerRegistration? = null
    private var lastLatLng: LatLng? = null
    
    private lateinit var modelHelper: MLModelHelper
    private val maliciousAppsList = mutableListOf<String>()
    private lateinit var maliciousAppsAdapter: MaliciousAppsAdapter

    private val usageHandler = Handler(Looper.getMainLooper())
    private val usageRunnable = object : Runnable {
        override fun run() {
            updateLocalUsage()
            usageHandler.postDelayed(this, 30000)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationService()
            enableMapsMyLocation()
            fetchLastKnownLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }
        val childId = user.uid

        modelHelper = MLModelHelper(this)
        
        initViews()
        setupBottomNav()
        setupMaps()
        setupProfileHeader(childId)
        setupLocationListener(childId)
        setupCommandListener(childId)
        
        checkPermissionsAndStartService()
        setupWorkManager()
        
        try {
            AppBehaviourAnalyzer.checkInstallAbuse()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking install abuse", e)
        }
    }

    private fun initViews() {
        statsView = findViewById(R.id.statsView)
        risksView = findViewById(R.id.risksView)
        locationView = findViewById(R.id.locationView)
        txtHeaderName = findViewById(R.id.txtHeaderName)
        txtAvatarLetter = findViewById(R.id.txtAvatarLetter)
        txtScreenTime = findViewById(R.id.txtScreenTime)
        
        imgApp1 = findViewById(R.id.imgApp1)
        imgApp2 = findViewById(R.id.imgApp2)
        imgApp3 = findViewById(R.id.imgApp3)

        // Risk View Elements
        scanPulseView = findViewById(R.id.scanPulseView)
        scanProgressIndicator = findViewById(R.id.scanProgressIndicator)
        txtScanPercent = findViewById(R.id.txtScanPercent)
        imgScanStatus = findViewById(R.id.imgScanStatus)
        btnScan = findViewById(R.id.btnScan)
        rvMaliciousApps = findViewById(R.id.rvMaliciousApps)
        txtRiskSummary = findViewById(R.id.txtRiskSummary)

        maliciousAppsAdapter = MaliciousAppsAdapter(maliciousAppsList)
        rvMaliciousApps.layoutManager = LinearLayoutManager(this)
        rvMaliciousApps.adapter = maliciousAppsAdapter
        
        btnScan.setOnClickListener {
            startScanAnimation()
        }

        findViewById<View>(R.id.cardUsage)?.setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }
    }

    private fun startScanAnimation() {
        btnScan.isEnabled = false
        imgScanStatus.visibility = View.GONE
        scanProgressIndicator.visibility = View.VISIBLE
        txtScanPercent.visibility = View.VISIBLE
        
        // Pulse Animation
        val pulse = ObjectAnimator.ofFloat(scanPulseView, "scaleX", 1f, 1.1f, 1f)
        pulse.duration = 1000
        pulse.repeatCount = ValueAnimator.INFINITE
        pulse.start()

        val pulseY = ObjectAnimator.ofFloat(scanPulseView, "scaleY", 1f, 1.1f, 1f)
        pulseY.duration = 1000
        pulseY.repeatCount = ValueAnimator.INFINITE
        pulseY.start()

        // Progress Animation
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 3000 // 3 seconds scan
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            scanProgressIndicator.progress = progress
            txtScanPercent.text = "$progress%"
        }
        
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                pulse.cancel()
                pulseY.cancel()
                scanPulseView.scaleX = 1f
                scanPulseView.scaleY = 1f
                performScan()
            }
        })
        animator.start()
    }

    private fun isTrustedSource(packageName: String): Boolean {
        val pm = packageManager
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
            
            val trustedInstallers = setOf(
                "com.android.vending",           // Google Play Store
                "com.google.android.feedback",   // Google
                "com.sec.android.app.samsungapps", // Samsung Galaxy Store
                "com.huawei.appmarket",          // Huawei AppGallery
                "com.xiaomi.mipicks",            // Xiaomi GetApps
                "com.miui.apkupdate",            // Xiaomi System Updates
                "com.oppo.market",               // Oppo App Market
                "com.coloros.oppostore",         // Oppo Store
                "com.vivo.appstore",             // Vivo V-Appstore
                "com.heytap.market",             // HeyTap (Oppo/Realme)
                "com.amazon.venezia"             // Amazon Appstore
            )
            
            trustedInstallers.contains(installer)
        } catch (e: Exception) {
            false
        }
    }

    private fun performScan() {
        maliciousAppsList.clear()
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var maliciousCount = 0

        for (app in apps) {
            // Skip all system apps and updated system apps (OS defaults)
            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystemApp || isUpdatedSystemApp) continue
            
            // Skip apps installed from official trusted sources
            if (isTrustedSource(app.packageName)) continue
            
            try {
                val pkgInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = pkgInfo.requestedPermissions
                val inputFeatures = FloatArray(DatasetPermissions.PERMISSIONS.size)
                
                requestedPermissions?.forEach { perm ->
                    val index = DatasetPermissions.PERMISSIONS.indexOf(perm)
                    if (index != -1) inputFeatures[index] = 1f
                }
                
                val prediction = modelHelper.predict(inputFeatures)
                val risk = (prediction * 100).toInt()
                
                if (risk >= 50) {
                    maliciousCount++
                    maliciousAppsList.add("${app.packageName},$risk,MALICIOUS")
                }
            } catch (e: Exception) {}
        }

        runOnUiThread {
            btnScan.isEnabled = true
            scanProgressIndicator.visibility = View.INVISIBLE
            txtScanPercent.visibility = View.GONE
            imgScanStatus.visibility = View.VISIBLE
            
            if (maliciousCount > 0) {
                imgScanStatus.setImageResource(R.drawable.ic_risk)
                imgScanStatus.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                txtRiskSummary.text = "Found $maliciousCount suspicious apps!"
                txtRiskSummary.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            } else {
                imgScanStatus.setImageResource(R.drawable.ic_check)
                imgScanStatus.setColorFilter(ContextCompat.getColor(this, R.color.google_green))
                txtRiskSummary.text = "Device is clean"
                txtRiskSummary.setTextColor(ContextCompat.getColor(this, R.color.google_green))
            }
            maliciousAppsAdapter.notifyDataSetChanged()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    showView(statsView)
                    true
                }
                R.id.nav_risks -> {
                    showView(risksView)
                    true
                }
                R.id.nav_location -> {
                    showView(locationView)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMaps() {
        val miniMapFragment = supportFragmentManager.findFragmentById(R.id.mapPreview) as? SupportMapFragment
        miniMapFragment?.getMapAsync { googleMap ->
            miniMap = googleMap
            miniMap?.uiSettings?.apply {
                isScrollGesturesEnabled = false
                isZoomGesturesEnabled = false
                isMapToolbarEnabled = false
                isMyLocationButtonEnabled = false
            }
            enableMapsMyLocation()
            lastLatLng?.let { updateMaps(it) }
        }

        val fullMapFragment = supportFragmentManager.findFragmentById(R.id.fullMap) as? SupportMapFragment
        fullMapFragment?.getMapAsync { googleMap ->
            fullMap = googleMap
            fullMap?.uiSettings?.isMyLocationButtonEnabled = true
            enableMapsMyLocation()
            lastLatLng?.let { updateMaps(it) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMapsMyLocation() {
        if (hasLocationPermission()) {
            miniMap?.isMyLocationEnabled = true
            fullMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocation() {
        if (hasLocationPermission()) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    lastLatLng = latLng
                    updateMaps(latLng)
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun showView(viewToShow: View) {
        statsView.visibility = View.GONE
        risksView.visibility = View.GONE
        locationView.visibility = View.GONE
        viewToShow.visibility = View.VISIBLE
    }

    private fun setupProfileHeader(uid: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isFinishing) return@addOnSuccessListener
                val name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "User"
                txtHeaderName.text = name
                txtAvatarLetter.text = name.take(1).uppercase()
            }
    }

    private fun updateLocalUsage() {
        if (!hasUsageStatsPermission()) {
            txtScreenTime.text = "0 min"
            return
        }

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        if (stats.isNullOrEmpty()) return

        val pm = packageManager
        val appUsageList = mutableListOf<Pair<String, Long>>()
        var totalMs = 0L

        val mainLauncher = getLauncherPackageName()

        for (s in stats) {
            if (s.totalTimeInForeground > 0) {
                // Skip system launcher and the app itself from usage stats
                if (s.packageName == mainLauncher || s.packageName == packageName) continue
                
                // Skip common system apps known to be launchers
                if (s.packageName.contains("launcher", ignoreCase = true) || 
                    s.packageName.contains("trebuchet", ignoreCase = true)) continue

                totalMs += s.totalTimeInForeground
                appUsageList.add(s.packageName to s.totalTimeInForeground)
            }
        }

        // Update Total Time
        val minutes = totalMs / 60000
        if (minutes >= 60) {
            val hours = minutes / 60
            val remMin = minutes % 60
            txtScreenTime.text = "${hours}h ${remMin}m"
        } else {
            txtScreenTime.text = "${minutes} min"
        }

        // Update Top 3 Apps
        val top3 = appUsageList.sortedByDescending { it.second }.take(3)
        val imageViews = listOf(imgApp1, imgApp2, imgApp3)
        
        imageViews.forEach { it.visibility = View.GONE }
        
        top3.forEachIndexed { index, pair ->
            try {
                val icon = pm.getApplicationIcon(pair.first)
                imageViews[index].setImageDrawable(icon)
                imageViews[index].visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icon for ${pair.first}", e)
            }
        }
    }

    private fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 
            android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun setupLocationListener(childId: String) {
        locationListener = FirebaseFirestore.getInstance()
            .collection("childLocations")
            .document(childId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) {
                    Log.e(TAG, "Location listener failed or no data", e)
                    return@addSnapshotListener
                }
                
                val lat = snapshot.getDouble("lat") ?: return@addSnapshotListener
                val lng = snapshot.getDouble("lng") ?: return@addSnapshotListener
                val latLng = LatLng(lat, lng)

                lastLatLng = latLng
                updateMaps(latLng)
            }
    }

    private fun updateMaps(latLng: LatLng) {
        miniMap?.let { map ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }

        fullMap?.let { map ->
            map.clear()
            map.addMarker(MarkerOptions().position(latLng).title("Current Location"))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    private fun setupCommandListener(childId: String) {
        FirebaseFirestore.getInstance()
            .collection("deviceCommands")
            .document(childId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val command = snapshot.getString("command") ?: return@addSnapshotListener
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
            fetchLastKnownLocation()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
        
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
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
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "dailyAnalytics",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        usageHandler.post(usageRunnable)
    }

    override fun onPause() {
        super.onPause()
        usageHandler.removeCallbacks(usageRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.remove()
    }

    override fun onMapReady(p0: GoogleMap) {
        // Handled in setupMaps
    }
}
