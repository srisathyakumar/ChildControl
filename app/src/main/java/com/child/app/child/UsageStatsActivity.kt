package com.child.app.child

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.child.app.R

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private val usageList =
        mutableListOf<Triple<String, String, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        recycler = findViewById(R.id.recyclerUsage)
        recycler.layoutManager = LinearLayoutManager(this)

        if (!hasPermission()) {
            startActivity(
                Intent(this, UsagePermissionActivity::class.java)
            )
            return
        }

        loadUsageStats()
    }

    private fun hasPermission(): Boolean {
        val appOps =
            getSystemService(Context.APP_OPS_SERVICE)
                    as android.app.AppOpsManager

        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )

        return mode ==
                android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun loadUsageStats() {

        val usm =
            getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - (24 * 60 * 60 * 1000)

        val stats =
            usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                start,
                end
            )

        val pm = packageManager

        usageList.clear()

        for (usage in stats) {

            if (usage.totalTimeInForeground > 0) {

                try {

                    val appInfo =
                        pm.getApplicationInfo(
                            usage.packageName,
                            0
                        )

                    val appName =
                        pm.getApplicationLabel(appInfo).toString()

                    usageList.add(
                        Triple(
                            usage.packageName,
                            appName,
                            usage.totalTimeInForeground
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        recycler.adapter =
            UsageAppsAdapter(usageList, pm)
    }

}
