package com.child.app.child

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChildDashboardViewModel(application: Application) : AndroidViewModel(application) {

    val userName = mutableStateOf("User")
    val avatarLetter = mutableStateOf("U")
    val avatarColor = mutableStateOf(androidx.compose.ui.graphics.Color(0xFF1D1B20))
    val profileImageUrl = mutableStateOf<String?>(null)
    
    val screenTimeToday = mutableStateOf("0 min")
    val lastUpdated = mutableStateOf("Last updated: --:--")
    val comparisonText = mutableStateOf("Calculating...")
    
    val hourlyUsage = mutableStateListOf<Float>()
    val topAppsUsage = mutableStateListOf<AppUsage>()
    
    data class AppUsage(val packageName: String, val minutes: Float, val icon: android.graphics.drawable.Drawable?)

    init {
        loadProfile()
        startUsageUpdates()
    }

    private fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: doc.getString("email")?.split("@")?.get(0) ?: "User"
                userName.value = name
                avatarLetter.value = name.take(1).uppercase()
                profileImageUrl.value = doc.getString("profileImage")
            }
    }

    private fun startUsageUpdates() {
        viewModelScope.launch {
            while (true) {
                updateAndSyncUsage()
                delay(15000) // Sync every 15 seconds
            }
        }
    }

    private fun updateAndSyncUsage() {
        val context = getApplication<Application>()
        val todayData = UsageHelper.getTodayUsageData(context)
        
        val minutes = todayData.totalMs / 60000
        screenTimeToday.value = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} m" else "$minutes min"
        lastUpdated.value = "Last updated: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        hourlyUsage.clear()
        hourlyUsage.addAll(todayData.hourlyUsage.toList())

        val pm = context.packageManager
        val sortedApps = todayData.appUsageMap.entries.sortedByDescending { it.value }.take(10)
        topAppsUsage.clear()
        
        val firestoreTopApps = mutableListOf<Map<String, Any>>()
        for (entry in sortedApps) {
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(entry.key, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) { entry.key }
            
            val icon = try { pm.getApplicationIcon(entry.key) } catch (e: Exception) { null }
            topAppsUsage.add(AppUsage(entry.key, entry.value / 60000f, icon))
            
            firestoreTopApps.add(mapOf(
                "pkg" to entry.key,
                "name" to appLabel,
                "time" to entry.value
            ))
        }

        val gamingMins = todayData.categoryUsage["Games"]?.div(60000) ?: 0L
        val socialMins = todayData.categoryUsage["Social Media"]?.div(60000) ?: 0L
        comparisonText.value = "Gaming: ${gamingMins}m | Social: ${socialMins}m"

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val syncData = mapOf(
            "totalTime" to todayData.totalMs,
            "topApps" to firestoreTopApps,
            "hourlyUsage" to todayData.hourlyUsage.toList(),
            "lastUpdated" to System.currentTimeMillis()
        )
        
        FirebaseFirestore.getInstance().collection("usage").document(uid).set(syncData)
    }
}
