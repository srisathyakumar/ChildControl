package com.child.app.child.service

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ChildUsageUploader(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadTodayUsage() {

        val childUid = auth.currentUser?.uid ?: return

        val usm =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - (1000 * 60 * 60 * 24)

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        if (stats.isNullOrEmpty()) return

        for (app in stats) {

            if (app.totalTimeInForeground > 0) {

                val packageName = app.packageName
                val timeUsed = app.totalTimeInForeground

                val data = hashMapOf(
                    "appPackage" to packageName,
                    "timeUsed" to timeUsed,
                    "lastUpdated" to Date()
                )

                firestore.collection("usage")
                    .document(childUid)
                    .collection("apps")
                    .document(packageName)
                    .set(data)
                    .addOnSuccessListener {

                        val minutesUsed =
                            timeUsed / 1000 / 60

                        checkLimit(
                            packageName,
                            minutesUsed
                        )
                    }
            }
        }

        Log.d("USAGE_UPLOAD", "Usage uploaded")
    }

    private fun checkLimit(
        packageName: String,
        minutesUsed: Long
    ) {

        val childId =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("screenLimits")
            .whereEqualTo("childId", childId)
            .whereEqualTo("packageName", packageName)
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {

                    val limit =
                        doc.getLong("limitMinutes")
                            ?: continue

                    if (minutesUsed > limit) {

                        sendOveruseAlert(
                            packageName,
                            minutesUsed,
                            limit
                        )
                    }
                }
            }
    }

    private fun sendOveruseAlert(
        packageName: String,
        used: Long,
        limit: Long
    ) {

        val data = hashMapOf(
            "packageName" to packageName,
            "timeUsed" to used,
            "limit" to limit,
            "childId" to FirebaseAuth
                .getInstance().currentUser?.uid,
            "timestamp" to Date()
        )

        FirebaseFirestore.getInstance()
            .collection("alerts")
            .add(data)
    }

}
