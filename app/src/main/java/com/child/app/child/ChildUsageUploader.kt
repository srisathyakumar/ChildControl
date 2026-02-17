package com.child.app.child

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
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

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000)

        val stats: List<UsageStats> =
            usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

        for (usage in stats) {

            val data = hashMapOf(
                "childId" to uid,
                "packageName" to usage.packageName,
                "timeUsed" to usage.totalTimeInForeground,
                "date" to System.currentTimeMillis()
            )

            firestore.collection("usage")
                .add(data)
        }
    }
}
