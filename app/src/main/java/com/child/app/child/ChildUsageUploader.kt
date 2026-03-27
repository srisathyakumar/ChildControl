package com.child.app.child

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ChildUsageUploader(
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "UsageUploader"

    fun uploadTodayUsage() {
        val childUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // 1. Get aggregated stats (Consolidated sessions)
        val aggregatedStats = usageManager.queryAndAggregateUsageStats(startTime, endTime)
        if (aggregatedStats.isNullOrEmpty()) return

        val pm = context.packageManager
        val usageMap = mutableMapOf<String, Long>()
        var totalTodayTime = 0L

        // 2. Identify the app currently in foreground to capture "Live" usage
        val liveSession = getLiveForegroundSession(usageManager, startTime, endTime)

        for ((pkg, stats) in aggregatedStats) {
            var time = stats.totalTimeInForeground
            
            // 🛡️ FIX: If this app is CURRENTLY open, add the duration of the current session
            if (liveSession != null && pkg == liveSession.first) {
                time += liveSession.second
                Log.d(tag, "Added live session for $pkg: ${liveSession.second / 1000}s")
            }

            if (time <= 0) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val hasLaunchIntent = pm.getLaunchIntentForPackage(pkg) != null
                
                if (isSystem && !hasLaunchIntent) continue
                if (pkg == context.packageName) continue

                usageMap[pkg] = time
                totalTodayTime += time

            } catch (e: Exception) { continue }
        }

        // 3. Prepare the optimized single-document summary
        val topApps = usageMap.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { entry ->
                val name = try {
                    val info = pm.getApplicationInfo(entry.key, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) { entry.key }
                
                mapOf("pkg" to entry.key, "name" to name, "time" to entry.value)
            }

        val usageSummary = hashMapOf(
            "totalTime" to totalTodayTime,
            "lastUpdated" to System.currentTimeMillis(),
            "topApps" to topApps,
            "dateLabel" to Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        )

        firestore.collection("usage").document(childUid)
            .set(usageSummary)
            .addOnSuccessListener {
                Log.d(tag, "Consolidated usage (including live) uploaded: ${totalTodayTime / 60000} mins")
                LimitManager(context).checkLimits(usageMap)
            }
    }

    /**
     * Finds the app currently in the foreground and returns its package name and
     * the time spent in the CURRENT unclosed session.
     */
    private fun getLiveForegroundSession(usageManager: UsageStatsManager, start: Long, end: Long): Pair<String, Long>? {
        val events = usageManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        var lastTime: Long = 0
        var isForeground = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastPkg = event.packageName
                    lastTime = event.timeStamp
                    isForeground = true
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    isForeground = false
                }
            }
        }

        return if (isForeground && lastPkg != null) {
            Pair(lastPkg, end - lastTime)
        } else null
    }
}
