package com.child.app.child

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.child.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private val usageList = mutableListOf<Triple<String, String, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        recycler = findViewById(R.id.recyclerUsage)
        recycler.layoutManager = LinearLayoutManager(this)

        if (!hasUsageStatsPermission(this)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Usage Access Granted", Toast.LENGTH_SHORT).show()
        }

        loadUsageStats()

        val request = PeriodicWorkRequestBuilder<DailyAnalyticsWorker>(1, TimeUnit.DAYS).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "dailyAnalytics",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadUsageStats() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - (24 * 60 * 60 * 1000)

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        )

        val pm = packageManager
        usageList.clear()

        val firestore = FirebaseFirestore.getInstance()
        val childId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        for (usage in stats) {
            if (usage.totalTimeInForeground > 0) {
                try {
                    val appInfo = pm.getApplicationInfo(usage.packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    usageList.add(
                        Triple(
                            usage.packageName,
                            appName,
                            usage.totalTimeInForeground
                        )
                    )

                    firestore.collection("usage")
                        .document(childId)
                        .collection("apps")
                        .document(usage.packageName)
                        .set(
                            mapOf(
                                "appName" to appName,
                                "package" to usage.packageName,
                                "time" to usage.totalTimeInForeground,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        recycler.adapter = UsageAppsAdapter(usageList, pm)
    }
}
