package com.child.app.child

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChildUsageUploader(
    private val context: Context
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadTodayUsage() {

        val uid = auth.currentUser?.uid ?: return

        val usageManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        // ✅ Today 12:00 AM start time
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats =
            usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

        val usageMap = mutableMapOf<String, Long>()

        for (usage in stats) {

            val pkg = usage.packageName
            val time = usage.totalTimeInForeground

            if (time > 0) {
                val current = usageMap[pkg] ?: 0L
                usageMap[pkg] = current + time
            }
        }

        for ((pkg, timeUsed) in usageMap) {

            val data = hashMapOf(
                "appPackage" to pkg,
                "timeUsed" to timeUsed,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("usage")
                .document(uid)
                .collection("apps")
                .document(pkg)
                .set(data)
        }
    }
}
