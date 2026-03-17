package com.child.app.child

import android.app.usage.UsageStatsManager
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import android.content.pm.ApplicationInfo
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ChildUsageUploader(
    private val context: Context
) {

    private val firestore = FirebaseFirestore.getInstance()

    fun uploadTodayUsage() {

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val role = prefs.getString("role", null)
        if (role != "child") return

        val childUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val usageManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        // 📅 Start of today (00:00)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) return

        val usageMap = mutableMapOf<String, Long>()
        val pm = context.packageManager
        var totalTodayTime = 0L

        for (usage in stats) {

            val pkg = usage.packageName
            val time = usage.totalTimeInForeground
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour in 0..5 && time > 0) {
                FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(childUid)
                    .collection("sleep")
                    .add(
                        mapOf(
                            "package" to pkg,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
            }

            if (time <= 0) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)

                // 🚫 Skip system apps
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

                // 🚫 Skip updated system apps
                if ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) continue

                // 🚫 Skip launcher explicitly
                if (pkg.contains("launcher")) continue

                // 🚫 Skip settings
                if (pkg.contains("settings")) continue

                // 🚫 Skip permission controller
                if (pkg.contains("permissioncontroller")) continue

                // Only user apps visible in launcher
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent == null) continue

                usageMap[pkg] = time
                totalTodayTime += time

            } catch (e: Exception) {
                continue
            }
        }

        // Upload total time
        firestore.collection("usage").document(childUid)
            .set(mapOf("totalTime" to totalTodayTime))

        // 🔥 Upload clean data only
        for ((pkg, timeUsed) in usageMap) {

            val data = hashMapOf(
                "appPackage" to pkg,
                "timeUsed" to timeUsed,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("usage")
                .document(childUid)
                .collection("apps")
                .document(pkg)
                .set(data)
        }

// ✅ CHECK LIMITS AFTER UPLOAD
        LimitManager(context).checkLimits(usageMap)
    }
}
